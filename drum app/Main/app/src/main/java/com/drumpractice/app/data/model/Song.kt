package com.drumpractice.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val id: Long = 0,
    val title: String,
    val artist: String? = null,
    val filePath: String,
    val duration: Long,
    val detectedBpm: Int? = null,
    val manualBpm: Int? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayed: Long? = null,
    val artworkPath: String? = null,
    val fileSize: Long = 0
) : Parcelable {
    val effectiveBpm: Int?
        get() = manualBpm ?: detectedBpm

    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
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
