package com.drumpractice.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * App settings stored in DataStore
 */
@Parcelize
data class AppSettings(
    // Audio settings
    val latencyOffsetMs: Int = 0,
    val clickStyle: ClickStyle = ClickStyle.STYLE_1,
    val defaultBpm: Int = 120,
    val defaultSubdivision: Subdivision = Subdivision.QUARTER,
    
    // Recording settings
    val countdownSeconds: Int = 4,
    val videoQuality: VideoQuality = VideoQuality.HD_720P,
    val audioSampleRate: Int = 44100,
    val recordMetronomeAudio: Boolean = false,
    
    // UI settings
    val theme: AppTheme = AppTheme.SYSTEM,
    val showBpmOnWaveform: Boolean = true,
    val hapticFeedback: Boolean = true,
    
    // Metronome defaults
    val defaultBeatsPerMeasure: Int = 4,
    val accentFirstBeat: Boolean = true,
    val defaultMetronomeVolume: Float = 0.8f,
    val defaultSubdivisionVolume: Float = 0.5f
) : Parcelable

@Parcelize
enum class AppTheme(val displayName: String) : Parcelable {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

@Parcelize
enum class VideoQuality(
    val displayName: String,
    val width: Int,
    val height: Int,
    val bitrate: Int
) : Parcelable {
    SD_480P("480p", 854, 480, 2_500_000),
    HD_720P("720p", 1280, 720, 5_000_000),
    FHD_1080P("1080p", 1920, 1080, 10_000_000)
}

@Parcelize
enum class AudioQuality(
    val displayName: String,
    val sampleRate: Int,
    val bitDepth: Int
) : Parcelable {
    STANDARD("Standard (44.1kHz)", 44100, 16),
    HIGH("High (48kHz)", 48000, 16),
    STUDIO("Studio (96kHz)", 96000, 24)
}
