package com.drumpractice.app.di

import android.content.Context
import androidx.room.Room
import com.drumpractice.app.data.local.DrumPracticeDatabase
import com.drumpractice.app.data.local.dao.PracticeSessionDao
import com.drumpractice.app.data.local.dao.RecordingDao
import com.drumpractice.app.data.local.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DrumPracticeDatabase {
        return Room.databaseBuilder(
            context,
            DrumPracticeDatabase::class.java,
            "drum_practice_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSongDao(database: DrumPracticeDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    fun provideRecordingDao(database: DrumPracticeDatabase): RecordingDao {
        return database.recordingDao()
    }

    @Provides
    fun providePracticeSessionDao(database: DrumPracticeDatabase): PracticeSessionDao {
        return database.practiceSessionDao()
    }
}
