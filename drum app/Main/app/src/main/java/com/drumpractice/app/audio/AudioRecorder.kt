package com.drumpractice.app.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio recorder that captures audio to WAV format.
 * Uses AudioRecord for low-level access and PCM data.
 */
@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class RecordingStatus(
        val isRecording: Boolean = false,
        val isPaused: Boolean = false,
        val duration: Long = 0,
        val audioLevel: Float = 0f,
        val filePath: String? = null
    )

    private val _status = MutableStateFlow(RecordingStatus())
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var outputFile: File? = null
    private var outputStream: FileOutputStream? = null
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Audio configuration
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    private var startTime: Long = 0
    private var pausedDuration: Long = 0
    private var pauseStartTime: Long = 0

    // Callback for audio level updates
    private var onAudioLevelUpdate: ((Float) -> Unit)? = null

    fun setOnAudioLevelUpdate(callback: ((Float) -> Unit)?) {
        onAudioLevelUpdate = callback
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording(outputPath: String): Boolean {
        if (_status.value.isRecording || !hasPermission()) {
            return false
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return false
            }

            outputFile = File(outputPath)
            outputFile?.parentFile?.mkdirs()
            outputStream = FileOutputStream(outputFile)

            // Write WAV header placeholder (will be updated when recording stops)
            writeWavHeader(outputStream!!, 0)

            audioRecord?.startRecording()
            startTime = System.currentTimeMillis()
            pausedDuration = 0

            _status.value = RecordingStatus(
                isRecording = true,
                isPaused = false,
                duration = 0,
                filePath = outputPath
            )

            startRecordingLoop()
            return true

        } catch (e: Exception) {
            e.printStackTrace()
            cleanup()
            return false
        }
    }

    private fun startRecordingLoop() {
        recordingJob = scope.launch {
            val buffer = ShortArray(bufferSize / 2)

            while (isActive && _status.value.isRecording) {
                if (_status.value.isPaused) {
                    delay(100)
                    continue
                }

                val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: -1

                if (readResult > 0) {
                    // Write PCM data
                    val byteBuffer = ByteBuffer.allocate(readResult * 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until readResult) {
                        byteBuffer.putShort(buffer[i])
                    }
                    outputStream?.write(byteBuffer.array())

                    // Calculate audio level for visualization
                    val level = calculateAudioLevel(buffer, readResult)

                    // Update duration
                    val currentDuration = System.currentTimeMillis() - startTime - pausedDuration

                    _status.value = _status.value.copy(
                        duration = currentDuration,
                        audioLevel = level
                    )

                    onAudioLevelUpdate?.invoke(level)
                }
            }
        }
    }

    private fun calculateAudioLevel(buffer: ShortArray, size: Int): Float {
        var sum = 0L
        for (i in 0 until size) {
            sum += buffer[i] * buffer[i]
        }
        val rms = kotlin.math.sqrt(sum.toDouble() / size)
        // Normalize to 0-1 range (Short.MAX_VALUE is max amplitude)
        return (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
    }

    fun pauseRecording() {
        if (_status.value.isRecording && !_status.value.isPaused) {
            pauseStartTime = System.currentTimeMillis()
            _status.value = _status.value.copy(isPaused = true)
        }
    }

    fun resumeRecording() {
        if (_status.value.isRecording && _status.value.isPaused) {
            pausedDuration += System.currentTimeMillis() - pauseStartTime
            _status.value = _status.value.copy(isPaused = false)
        }
    }

    fun stopRecording(): String? {
        if (!_status.value.isRecording) {
            return null
        }

        recordingJob?.cancel()
        recordingJob = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        // Update WAV header with final file size
        outputStream?.close()
        outputFile?.let { file ->
            updateWavHeader(file)
        }

        val filePath = _status.value.filePath

        _status.value = RecordingStatus()

        return filePath
    }

    fun cancelRecording() {
        recordingJob?.cancel()
        recordingJob = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        outputStream?.close()
        outputFile?.delete()

        _status.value = RecordingStatus()
    }

    private fun writeWavHeader(outputStream: FileOutputStream, dataSize: Int) {
        val totalSize = 36 + dataSize
        val header = ByteBuffer.allocate(44)
        header.order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        header.put("RIFF".toByteArray())
        header.putInt(totalSize)
        header.put("WAVE".toByteArray())

        // fmt subchunk
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1) // AudioFormat (1 = PCM)
        header.putShort(1) // NumChannels (mono)
        header.putInt(sampleRate) // SampleRate
        header.putInt(sampleRate * 2) // ByteRate (SampleRate * NumChannels * BitsPerSample/8)
        header.putShort(2) // BlockAlign (NumChannels * BitsPerSample/8)
        header.putShort(16) // BitsPerSample

        // data subchunk
        header.put("data".toByteArray())
        header.putInt(dataSize)

        outputStream.write(header.array())
    }

    private fun updateWavHeader(file: File) {
        try {
            val randomAccessFile = RandomAccessFile(file, "rw")
            val fileSize = file.length().toInt()
            val dataSize = fileSize - 44

            // Update RIFF chunk size
            randomAccessFile.seek(4)
            randomAccessFile.write(intToByteArrayLE(36 + dataSize))

            // Update data chunk size
            randomAccessFile.seek(40)
            randomAccessFile.write(intToByteArrayLE(dataSize))

            randomAccessFile.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun intToByteArrayLE(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun cleanup() {
        audioRecord?.release()
        audioRecord = null
        outputStream?.close()
        outputStream = null
        outputFile = null
    }

    fun release() {
        cancelRecording()
        scope.cancel()
    }
}
