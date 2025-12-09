package com.drumpractice.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practice_sessions")
data class PracticeSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: Long? = null,
    val recordingId: Long? = null,
    val bpmUsed: Int,
    val subdivisionUsed: String,
    val durationSeconds: Int,
    val date: Long = System.currentTimeMillis(),
    val notes: String? = null
)
