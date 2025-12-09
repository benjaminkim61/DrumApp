package com.drumpractice.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.drumpractice.app.data.local.dao.PracticeSessionDao
import com.drumpractice.app.data.local.dao.RecordingDao
import com.drumpractice.app.data.local.dao.SongDao
import com.drumpractice.app.data.local.entity.PracticeSessionEntity
import com.drumpractice.app.data.local.entity.RecordingEntity
import com.drumpractice.app.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        RecordingEntity::class,
        PracticeSessionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class DrumPracticeDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun recordingDao(): RecordingDao
    abstract fun practiceSessionDao(): PracticeSessionDao
}
