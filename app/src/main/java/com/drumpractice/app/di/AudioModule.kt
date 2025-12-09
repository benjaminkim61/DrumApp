package com.drumpractice.app.di

import android.content.Context
import com.drumpractice.app.audio.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideMetronomeEngine(@ApplicationContext context: Context): MetronomeEngine {
        return MetronomeEngine(context)
    }

    @Provides
    @Singleton
    fun provideAudioPlayer(@ApplicationContext context: Context): AudioPlayer {
        return AudioPlayer(context)
    }

    @Provides
    @Singleton
    fun provideAudioRecorder(@ApplicationContext context: Context): AudioRecorder {
        return AudioRecorder(context)
    }

    @Provides
    @Singleton
    fun provideVideoRecorder(@ApplicationContext context: Context): VideoRecorder {
        return VideoRecorder(context)
    }

    @Provides
    @Singleton
    fun provideBpmDetector(@ApplicationContext context: Context): BpmDetector {
        return BpmDetector(context)
    }
}
