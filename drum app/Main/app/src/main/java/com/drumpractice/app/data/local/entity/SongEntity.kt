package com.drumpractice.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String? = null,
    val filePath: String,
    val duration: Long, // in milliseconds
    val detectedBpm: Int? = null,
    val manualBpm: Int? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayed: Long? = null,
    val artworkPath: String? = null,
    val fileSize: Long = 0
)
