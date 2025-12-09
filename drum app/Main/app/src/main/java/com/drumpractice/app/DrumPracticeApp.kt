package com.drumpractice.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DrumPracticeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Playback channel
            val playbackChannel = NotificationChannel(
                CHANNEL_PLAYBACK,
                getString(R.string.notification_channel_playback),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Audio playback controls"
                setShowBadge(false)
            }

            // Recording channel
            val recordingChannel = NotificationChannel(
                CHANNEL_RECORDING,
                getString(R.string.notification_channel_recording),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recording status and controls"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(playbackChannel, recordingChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_PLAYBACK = "playback_channel"
        const val CHANNEL_RECORDING = "recording_channel"
    }
}
