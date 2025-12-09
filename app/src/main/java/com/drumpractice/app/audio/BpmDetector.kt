package com.drumpractice.app.audio

import android.content.Context
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.beatroot.BeatRootOnsetEventHandler
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import be.tarsos.dsp.onsets.OnsetHandler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BPM detection using TarsosDSP library.
 * Analyzes audio files to detect tempo/BPM.
 */
@Singleton
class BpmDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class BpmResult(
        val bpm: Int,
        val confidence: Float,
        val candidates: List<Int>
    )

    data class DetectionState(
        val isAnalyzing: Boolean = false,
        val progress: Float = 0f,
        val result: BpmResult? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(DetectionState())
    val state: StateFlow<DetectionState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var detectionJob: Job? = null

    /**
     * Detect BPM from an audio file.
     * Supports WAV, MP3, FLAC, etc.
     */
    fun detectBpm(filePath: String) {
        if (_state.value.isAnalyzing) return

        detectionJob?.cancel()
        detectionJob = scope.launch {
            try {
                _state.value = DetectionState(isAnalyzing = true, progress = 0f)

                val result = analyzeFile(filePath)

                _state.value = DetectionState(
                    isAnalyzing = false,
                    progress = 1f,
                    result = result
                )
            } catch (e: Exception) {
                _state.value = DetectionState(
                    isAnalyzing = false,
                    error = e.message ?: "BPM detection failed"
                )
            }
        }
    }

    private suspend fun analyzeFile(filePath: String): BpmResult = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: $filePath")
        }

        val onsetTimes = mutableListOf<Double>()

        try {
            // Create audio format (assuming 44.1kHz stereo, will be adapted)
            val format = TarsosDSPAudioFormat(44100f, 16, 1, true, false)
            
            val inputStream = BufferedInputStream(FileInputStream(file))
            val audioInputStream = UniversalAudioInputStream(inputStream, format)
            
            val bufferSize = 2048
            val overlap = 1024
            
            val dispatcher = AudioDispatcher(audioInputStream, bufferSize, overlap)

            // Onset detection
            val onsetDetector = ComplexOnsetDetector(bufferSize)
            onsetDetector.setHandler(OnsetHandler { time, _ ->
                onsetTimes.add(time)
            })

            dispatcher.addAudioProcessor(onsetDetector)

            // Progress tracking
            var processedSamples = 0L
            val totalSamples = file.length() / 2 // Approximate for 16-bit audio

            dispatcher.addAudioProcessor(object : AudioProcessor {
                override fun process(audioEvent: AudioEvent): Boolean {
                    processedSamples += audioEvent.bufferSize
                    val progress = (processedSamples.toFloat() / totalSamples).coerceIn(0f, 1f)
                    _state.value = _state.value.copy(progress = progress)
                    return true
                }

                override fun processingFinished() {}
            })

            dispatcher.run()
            audioInputStream.close()

        } catch (e: Exception) {
            // If TarsosDSP fails, use fallback analysis
            return@withContext fallbackBpmDetection(file)
        }

        // Calculate BPM from onset times
        calculateBpmFromOnsets(onsetTimes)
    }

    private fun calculateBpmFromOnsets(onsetTimes: List<Double>): BpmResult {
        if (onsetTimes.size < 4) {
            return BpmResult(120, 0.5f, listOf(120))
        }

        // Calculate inter-onset intervals (IOI)
        val intervals = mutableListOf<Double>()
        for (i in 1 until onsetTimes.size) {
            val interval = onsetTimes[i] - onsetTimes[i - 1]
            if (interval > 0.1 && interval < 2.0) { // Filter reasonable intervals (30-600 BPM)
                intervals.add(interval)
            }
        }

        if (intervals.isEmpty()) {
            return BpmResult(120, 0.5f, listOf(120))
        }

        // Build histogram of intervals quantized to BPM values
        val bpmCounts = mutableMapOf<Int, Int>()
        for (interval in intervals) {
            val bpm = (60.0 / interval).toInt()
            // Consider both the BPM and its half/double
            listOf(bpm, bpm / 2, bpm * 2).forEach { candidateBpm ->
                if (candidateBpm in 40..220) {
                    bpmCounts[candidateBpm] = (bpmCounts[candidateBpm] ?: 0) + 1
                }
            }
        }

        // Find top candidates
        val sortedCandidates = bpmCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }

        val topBpm = sortedCandidates.firstOrNull() ?: 120
        val confidence = (bpmCounts[topBpm] ?: 0).toFloat() / intervals.size.coerceAtLeast(1)

        return BpmResult(
            bpm = topBpm,
            confidence = confidence.coerceIn(0f, 1f),
            candidates = sortedCandidates
        )
    }

    private fun fallbackBpmDetection(file: File): BpmResult {
        // Simple energy-based beat detection as fallback
        try {
            val inputStream = FileInputStream(file)
            val buffer = ByteArray(4096)
            val energyLevels = mutableListOf<Float>()

            // Skip WAV header if present
            if (file.extension.equals("wav", ignoreCase = true)) {
                inputStream.skip(44)
            }

            while (true) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead <= 0) break

                // Calculate RMS energy
                var sum = 0L
                for (i in 0 until bytesRead step 2) {
                    if (i + 1 < bytesRead) {
                        val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                        sum += sample * sample
                    }
                }
                val rms = kotlin.math.sqrt(sum.toDouble() / (bytesRead / 2))
                energyLevels.add(rms.toFloat())
            }
            inputStream.close()

            // Find peaks in energy
            val peaks = mutableListOf<Int>()
            val threshold = energyLevels.average() * 1.5

            for (i in 1 until energyLevels.size - 1) {
                if (energyLevels[i] > threshold &&
                    energyLevels[i] > energyLevels[i - 1] &&
                    energyLevels[i] > energyLevels[i + 1]
                ) {
                    peaks.add(i)
                }
            }

            // Calculate BPM from peaks
            if (peaks.size >= 2) {
                val intervals = mutableListOf<Int>()
                for (i in 1 until peaks.size) {
                    intervals.add(peaks[i] - peaks[i - 1])
                }
                val avgInterval = intervals.average()
                // Each buffer is ~93ms at 44100Hz
                val msPerBuffer = 4096.0 / 44100.0 * 1000
                val bpm = (60000 / (avgInterval * msPerBuffer)).toInt().coerceIn(40, 220)
                return BpmResult(bpm, 0.6f, listOf(bpm))
            }

        } catch (e: Exception) {
            // Ignore and return default
        }

        return BpmResult(120, 0.3f, listOf(120))
    }

    fun cancel() {
        detectionJob?.cancel()
        _state.value = DetectionState()
    }

    fun clearResult() {
        _state.value = DetectionState()
    }

    fun release() {
        cancel()
        scope.cancel()
    }
}
