package com.drumpractice.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import com.drumpractice.app.data.model.ClickStyle
import com.drumpractice.app.data.model.MetronomeState
import com.drumpractice.app.data.model.Subdivision
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Metronome engine that generates and plays click sounds with subdivisions.
 * Uses SoundPool for loaded MP3 clicks and AudioTrack for synthesized clicks.
 */
@Singleton
class MetronomeEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _state = MutableStateFlow(MetronomeState())
    val state: StateFlow<MetronomeState> = _state.asStateFlow()

    private var soundPool: SoundPool? = null
    private var clickSoundId: Int = 0
    private var accentSoundId: Int = 0
    private var subdivisionSoundId: Int = 0

    private val audioExecutor = Executors.newSingleThreadExecutor()
    private var metronomeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Callback for beat events (for UI animations, etc.)
    private var onBeatCallback: ((Int, Boolean, Boolean) -> Unit)? = null // beat, isAccent, isSubdivision

    // Latency compensation
    private var latencyOffsetMs: Long = 0

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        loadClickSounds(_state.value.clickStyle)
    }

    fun loadClickSounds(style: ClickStyle) {
        soundPool?.let { pool ->
            try {
                // Load main click
                context.assets.open("metronome/${style.clickFileName}").use {
                    clickSoundId = pool.load(context.assets.openFd("metronome/${style.clickFileName}"), 1)
                }
            } catch (e: Exception) {
                // Fall back to synthesized click
                clickSoundId = 0
            }

            try {
                // Load accent click
                context.assets.open("metronome/${style.accentFileName}").use {
                    accentSoundId = pool.load(context.assets.openFd("metronome/${style.accentFileName}"), 1)
                }
            } catch (e: Exception) {
                accentSoundId = 0
            }

            try {
                // Load subdivision click
                context.assets.open("metronome/${style.subdivisionFileName}").use {
                    subdivisionSoundId = pool.load(context.assets.openFd("metronome/${style.subdivisionFileName}"), 1)
                }
            } catch (e: Exception) {
                subdivisionSoundId = 0
            }
        }

        _state.value = _state.value.copy(clickStyle = style)
    }

    fun setOnBeatCallback(callback: ((Int, Boolean, Boolean) -> Unit)?) {
        onBeatCallback = callback
    }

    fun setLatencyOffset(offsetMs: Long) {
        latencyOffsetMs = offsetMs
    }

    fun setBpm(bpm: Int) {
        val clampedBpm = bpm.coerceIn(MetronomeState.MIN_BPM, MetronomeState.MAX_BPM)
        _state.value = _state.value.copy(bpm = clampedBpm)
    }

    fun setSubdivision(subdivision: Subdivision) {
        _state.value = _state.value.copy(subdivision = subdivision)
    }

    fun setBeatsPerMeasure(beats: Int) {
        _state.value = _state.value.copy(beatsPerMeasure = beats.coerceIn(1, 12))
    }

    fun setVolume(volume: Float) {
        _state.value = _state.value.copy(volume = volume.coerceIn(0f, 1f))
    }

    fun setSubdivisionVolume(volume: Float) {
        _state.value = _state.value.copy(subdivisionVolume = volume.coerceIn(0f, 1f))
    }

    fun setAccentFirstBeat(accent: Boolean) {
        _state.value = _state.value.copy(accentFirstBeat = accent)
    }

    fun start() {
        if (_state.value.isPlaying) return

        _state.value = _state.value.copy(isPlaying = true, currentBeat = 0, currentSubdivision = 0)
        startMetronomeLoop()
    }

    fun stop() {
        _state.value = _state.value.copy(isPlaying = false, currentBeat = 0, currentSubdivision = 0)
        metronomeJob?.cancel()
        metronomeJob = null
    }

    fun toggle() {
        if (_state.value.isPlaying) stop() else start()
    }

    private fun startMetronomeLoop() {
        metronomeJob?.cancel()
        metronomeJob = scope.launch {
            var beatCounter = 0
            var subdivisionCounter = 0

            while (isActive && _state.value.isPlaying) {
                val currentState = _state.value
                val subdivision = currentState.subdivision
                val clicksPerBeat = subdivision.clicksPerBeat
                val isSwing = subdivision == Subdivision.SWING

                // Calculate timing
                val beatIntervalMs = currentState.intervalMs
                val subdivisionIntervalMs = if (isSwing) {
                    // Swing: first subdivision is 2/3, second is 1/3
                    if (subdivisionCounter % 2 == 0) {
                        (beatIntervalMs * 2) / 3
                    } else {
                        beatIntervalMs / 3
                    }
                } else {
                    beatIntervalMs / clicksPerBeat
                }

                // Determine click type
                val isMainBeat = subdivisionCounter == 0
                val isAccent = isMainBeat && beatCounter == 0 && currentState.accentFirstBeat

                // Play sound
                playClick(isAccent, isMainBeat, currentState.volume, currentState.subdivisionVolume, subdivision.pitchMultiplier)

                // Update state
                _state.value = currentState.copy(
                    currentBeat = beatCounter,
                    currentSubdivision = subdivisionCounter
                )

                // Notify callback
                onBeatCallback?.invoke(beatCounter, isAccent, !isMainBeat)

                // Wait for next click
                delay(subdivisionIntervalMs - latencyOffsetMs.coerceAtLeast(0))

                // Increment counters
                subdivisionCounter++
                if (subdivisionCounter >= clicksPerBeat) {
                    subdivisionCounter = 0
                    beatCounter = (beatCounter + 1) % currentState.beatsPerMeasure
                }
            }
        }
    }

    private fun playClick(
        isAccent: Boolean,
        isMainBeat: Boolean,
        volume: Float,
        subdivisionVolume: Float,
        pitchMultiplier: Float
    ) {
        soundPool?.let { pool ->
            when {
                isAccent && accentSoundId != 0 -> {
                    pool.play(accentSoundId, volume, volume, 1, 0, 1.0f)
                }
                isMainBeat && clickSoundId != 0 -> {
                    pool.play(clickSoundId, volume, volume, 1, 0, 1.0f)
                }
                !isMainBeat && subdivisionSoundId != 0 -> {
                    pool.play(subdivisionSoundId, subdivisionVolume, subdivisionVolume, 1, 0, pitchMultiplier)
                }
                else -> {
                    // Fallback: synthesize click
                    playSynthesizedClick(isAccent, isMainBeat, volume, subdivisionVolume, pitchMultiplier)
                }
            }
        } ?: run {
            playSynthesizedClick(isAccent, isMainBeat, volume, subdivisionVolume, pitchMultiplier)
        }
    }

    private fun playSynthesizedClick(
        isAccent: Boolean,
        isMainBeat: Boolean,
        volume: Float,
        subdivisionVolume: Float,
        pitchMultiplier: Float
    ) {
        audioExecutor.execute {
            val clickVolume = if (isMainBeat) volume else subdivisionVolume
            val frequency = when {
                isAccent -> 1200f
                isMainBeat -> 1000f
                else -> 800f * pitchMultiplier
            }
            val durationMs = if (isMainBeat) 30 else 20

            synthesizeAndPlayClick(frequency, durationMs, clickVolume)
        }
    }

    private fun synthesizeAndPlayClick(frequency: Float, durationMs: Int, volume: Float) {
        val sampleRate = 44100
        val numSamples = (sampleRate * durationMs) / 1000
        val samples = ShortArray(numSamples)

        // Generate sine wave with exponential decay envelope
        for (i in 0 until numSamples) {
            val time = i.toFloat() / sampleRate
            val envelope = kotlin.math.exp(-time * 50) // Fast decay
            val sample = (kotlin.math.sin(2 * Math.PI * frequency * time) * envelope * volume * Short.MAX_VALUE).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()

            // Release after playback
            Thread.sleep(durationMs.toLong() + 10)
            audioTrack.release()
        } catch (e: Exception) {
            // Ignore audio playback errors
        }
    }

    fun release() {
        stop()
        scope.cancel()
        soundPool?.release()
        soundPool = null
        audioExecutor.shutdown()
    }

    // Tap tempo functionality
    private val tapTimes = mutableListOf<Long>()
    private val maxTapHistory = 8

    fun tapTempo(): Int {
        val now = System.currentTimeMillis()

        // Clear old taps (more than 2 seconds gap)
        if (tapTimes.isNotEmpty() && now - tapTimes.last() > 2000) {
            tapTimes.clear()
        }

        tapTimes.add(now)

        // Keep only recent taps
        while (tapTimes.size > maxTapHistory) {
            tapTimes.removeAt(0)
        }

        // Calculate BPM from tap intervals
        if (tapTimes.size >= 2) {
            val intervals = mutableListOf<Long>()
            for (i in 1 until tapTimes.size) {
                intervals.add(tapTimes[i] - tapTimes[i - 1])
            }
            val averageInterval = intervals.average()
            val calculatedBpm = (60000 / averageInterval).toInt()
                .coerceIn(MetronomeState.MIN_BPM, MetronomeState.MAX_BPM)

            setBpm(calculatedBpm)
            return calculatedBpm
        }

        return _state.value.bpm
    }

    fun clearTapTempo() {
        tapTimes.clear()
    }
}
