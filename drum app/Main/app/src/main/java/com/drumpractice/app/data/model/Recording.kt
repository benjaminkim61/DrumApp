package com.drumpractice.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recording(
    val id: Long = 0,
    val name: String,
    val filePath: String,
    val thumbnailPath: String? = null,
    val duration: Long,
    val isVideo: Boolean = false,
    val songId: Long? = null,
    val bpmUsed: Int? = null,
    val subdivisionUsed: Subdivision? = null,
    val audioDelay: Long = 0,
    val recordingVolume: Float = 1.0f,
    val backtrackVolume: Float = 1.0f,
    val metronomeVolume: Float = 1.0f,
    val dateCreated: Long = System.currentTimeMillis(),
    val fileSize: Long = 0
) : Parcelable {
    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val formattedDate: String
        get() {
            val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(dateCreated))
        }

    val formattedFileSize: String
        get() {
            return when {
                fileSize < 1024 -> "$fileSize B"
                fileSize < 1024 * 1024 -> "%.1f KB".format(fileSize / 1024.0)
                else -> "%.1f MB".format(fileSize / (1024.0 * 1024.0))
            }
        }
}
