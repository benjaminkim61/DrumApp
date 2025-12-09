package com.drumpractice.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.drumpractice.app.DrumPracticeApp
import com.drumpractice.app.R
import com.drumpractice.app.audio.AudioRecorder
import com.drumpractice.app.audio.VideoRecorder
import com.drumpractice.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var audioRecorder: AudioRecorder

    @Inject
    lateinit var videoRecorder: VideoRecorder

    companion object {
        const val ACTION_START_AUDIO = "START_AUDIO_RECORDING"
        const val ACTION_START_VIDEO = "START_VIDEO_RECORDING"
        const val ACTION_STOP = "STOP_RECORDING"
        const val ACTION_PAUSE = "PAUSE_RECORDING"
        const val ACTION_RESUME = "RESUME_RECORDING"
        const val EXTRA_OUTPUT_PATH = "output_path"

        private const val NOTIFICATION_ID = 1001
    }

    private var isRecordingAudio = false
    private var isRecordingVideo = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_AUDIO -> {
                val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH) ?: return START_NOT_STICKY
                startAudioRecording(outputPath)
            }
            ACTION_START_VIDEO -> {
                val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH) ?: return START_NOT_STICKY
                startVideoRecording(outputPath)
            }
            ACTION_STOP -> stopRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
        }
        return START_STICKY
    }

    private fun startAudioRecording(outputPath: String) {
        startForeground(NOTIFICATION_ID, createNotification("Recording audio..."))
        isRecordingAudio = audioRecorder.startRecording(outputPath)
    }

    private fun startVideoRecording(outputPath: String) {
        startForeground(NOTIFICATION_ID, createNotification("Recording video..."))
        isRecordingVideo = videoRecorder.startRecording(outputPath)
    }

    private fun stopRecording() {
        if (isRecordingAudio) {
            audioRecorder.stopRecording()
            isRecordingAudio = false
        }
        if (isRecordingVideo) {
            videoRecorder.stopRecording()
            isRecordingVideo = false
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pauseRecording() {
        if (isRecordingAudio) {
            audioRecorder.pauseRecording()
        }
        if (isRecordingVideo) {
            videoRecorder.pauseRecording()
        }
    }

    private fun resumeRecording() {
        if (isRecordingAudio) {
            audioRecorder.resumeRecording()
        }
        if (isRecordingVideo) {
            videoRecorder.resumeRecording()
        }
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, DrumPracticeApp.CHANNEL_RECORDING)
            .setContentTitle(getString(R.string.notification_recording_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        if (isRecordingAudio) {
            audioRecorder.stopRecording()
        }
        if (isRecordingVideo) {
            videoRecorder.stopRecording()
        }
        super.onDestroy()
    }
}
