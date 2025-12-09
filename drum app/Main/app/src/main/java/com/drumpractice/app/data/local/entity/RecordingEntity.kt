package com.drumpractice.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recordings",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("songId")]
)
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val filePath: String,
    val thumbnailPath: String? = null,
    val duration: Long, // in milliseconds
    val isVideo: Boolean = false,
    val songId: Long? = null, // associated backing track
    val bpmUsed: Int? = null,
    val subdivisionUsed: String? = null,
    val audioDelay: Long = 0, // user-adjusted delay in ms
    val recordingVolume: Float = 1.0f,
    val backtrackVolume: Float = 1.0f,
    val metronomeVolume: Float = 1.0f,
    val dateCreated: Long = System.currentTimeMillis(),
    val fileSize: Long = 0
)
