package com.drumpractice.app.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import com.drumpractice.app.data.model.VideoQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Video recorder using CameraX for capturing video with audio.
 */
@Singleton
class VideoRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class VideoRecordingStatus(
        val isRecording: Boolean = false,
        val isPaused: Boolean = false,
        val duration: Long = 0,
        val filePath: String? = null,
        val error: String? = null
    )

    private val _status = MutableStateFlow(VideoRecordingStatus())
    val status: StateFlow<VideoRecordingStatus> = _status.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var camera: Camera? = null
    private var preview: Preview? = null

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var currentOutputFile: File? = null

    private var videoQuality = VideoQuality.HD_720P

    fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == 
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == 
            PackageManager.PERMISSION_GRANTED
    }

    fun setVideoQuality(quality: VideoQuality) {
        videoQuality = quality
    }

    fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onInitialized: (Boolean) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
                onInitialized(true)
            } catch (e: Exception) {
                _status.value = _status.value.copy(error = "Failed to initialize camera: ${e.message}")
                onInitialized(false)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProvider = cameraProvider ?: return

        // Camera selector - prefer front camera for drummer self-recording
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        // Preview
        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // Video capture quality selection
        val qualitySelector = QualitySelector.from(
            when (videoQuality) {
                VideoQuality.SD_480P -> Quality.SD
                VideoQuality.HD_720P -> Quality.HD
                VideoQuality.FHD_1080P -> Quality.FHD
            },
            FallbackStrategy.higherQualityOrLowerThan(Quality.HD)
        )

        // Recorder
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )
        } catch (e: Exception) {
            _status.value = _status.value.copy(error = "Failed to bind camera: ${e.message}")
        }
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraProvider?.let { provider ->
            val currentSelector = camera?.cameraInfo?.let { info ->
                when {
                    info.lensFacing == CameraSelector.LENS_FACING_FRONT -> CameraSelector.DEFAULT_BACK_CAMERA
                    else -> CameraSelector.DEFAULT_FRONT_CAMERA
                }
            } ?: CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    currentSelector,
                    preview,
                    videoCapture
                )
            } catch (e: Exception) {
                _status.value = _status.value.copy(error = "Failed to switch camera: ${e.message}")
            }
        }
    }

    fun startRecording(outputPath: String): Boolean {
        if (_status.value.isRecording || !hasPermissions()) {
            return false
        }

        val videoCapture = videoCapture ?: return false

        currentOutputFile = File(outputPath)
        currentOutputFile?.parentFile?.mkdirs()

        val outputOptions = FileOutputOptions.Builder(currentOutputFile!!)
            .build()

        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(cameraExecutor, createRecordingListener())

        _status.value = VideoRecordingStatus(
            isRecording = true,
            isPaused = false,
            duration = 0,
            filePath = outputPath
        )

        return true
    }

    private fun createRecordingListener(): Consumer<VideoRecordEvent> {
        return Consumer { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    _status.value = _status.value.copy(isRecording = true)
                }
                is VideoRecordEvent.Finalize -> {
                    if (event.hasError()) {
                        _status.value = _status.value.copy(
                            isRecording = false,
                            error = "Recording error: ${event.error}"
                        )
                    } else {
                        _status.value = _status.value.copy(
                            isRecording = false,
                            filePath = event.outputResults.outputUri.path
                        )
                    }
                }
                is VideoRecordEvent.Status -> {
                    val durationNanos = event.recordingStats.recordedDurationNanos
                    _status.value = _status.value.copy(
                        duration = durationNanos / 1_000_000 // Convert to milliseconds
                    )
                }
                is VideoRecordEvent.Pause -> {
                    _status.value = _status.value.copy(isPaused = true)
                }
                is VideoRecordEvent.Resume -> {
                    _status.value = _status.value.copy(isPaused = false)
                }
            }
        }
    }

    fun pauseRecording() {
        recording?.pause()
    }

    fun resumeRecording() {
        recording?.resume()
    }

    fun stopRecording(): String? {
        recording?.stop()
        recording = null

        val filePath = _status.value.filePath
        _status.value = VideoRecordingStatus()

        return filePath
    }

    fun cancelRecording() {
        recording?.stop()
        recording = null

        currentOutputFile?.delete()
        currentOutputFile = null

        _status.value = VideoRecordingStatus()
    }

    fun enableTorch(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
    }

    fun release() {
        cancelRecording()
        cameraProvider?.unbindAll()
        cameraProvider = null
        cameraExecutor.shutdown()
    }
}
