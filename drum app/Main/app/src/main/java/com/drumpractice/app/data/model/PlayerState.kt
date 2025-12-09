package com.drumpractice.app.data.model

/**
 * Player state for audio playback
 */
data class PlayerState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val currentSong: Song? = null,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isLooping: Boolean = false,
    val loopStartMs: Long? = null,
    val loopEndMs: Long? = null
) {
    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f

    val remainingTime: Long
        get() = maxOf(0, duration - currentPosition)

    val formattedPosition: String
        get() = formatTime(currentPosition)

    val formattedDuration: String
        get() = formatTime(duration)

    val formattedRemaining: String
        get() = "-${formatTime(remainingTime)}"

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}

/**
 * Recording state
 */
data class RecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val recordingDuration: Long = 0,
    val isCountingDown: Boolean = false,
    val countdownValue: Int = 0,
    val isVideo: Boolean = false,
    val audioLevel: Float = 0f, // 0-1 range for visualizing input level
    val hasBackingTrack: Boolean = false,
    val hasMetronome: Boolean = false
) {
    val formattedDuration: String
        get() {
            val totalSeconds = recordingDuration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val millis = (recordingDuration % 1000) / 10
            return "%02d:%02d.%02d".format(minutes, seconds, millis)
        }
}

/**
 * Post-recording adjustment state
 */
data class PostRecordingState(
    val recording: Recording? = null,
    val audioDelayMs: Long = 0,
    val recordingVolume: Float = 1.0f,
    val backtrackVolume: Float = 1.0f,
    val metronomeVolume: Float = 1.0f,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val hasUnsavedChanges: Boolean = false
)
