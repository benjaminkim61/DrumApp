package com.drumpractice.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents different subdivision types for the metronome.
 * Each subdivision has a multiplier that determines how many clicks
 * per beat and a display name.
 */
@Parcelize
enum class Subdivision(
    val displayName: String,
    val clicksPerBeat: Int,
    val pitchMultiplier: Float // Lower pitch for subdivisions
) : Parcelable {
    QUARTER("Quarter", 1, 1.0f),
    EIGHTH("8th", 2, 0.8f),
    EIGHTH_TRIPLET("8th Triplet", 3, 0.75f),
    SIXTEENTH("16th", 4, 0.7f),
    SIXTEENTH_TRIPLET("16th Triplet", 6, 0.65f),
    THIRTY_SECOND("32nd", 8, 0.6f),
    SWING("Swing", 2, 0.8f); // Special timing for swing feel

    companion object {
        fun fromString(value: String): Subdivision {
            return entries.find { it.name == value } ?: QUARTER
        }

        fun getAll(): List<Subdivision> = entries
    }
}

/**
 * Click style variations for the metronome
 */
@Parcelize
enum class ClickStyle(
    val displayName: String,
    val clickFileName: String,
    val accentFileName: String,
    val subdivisionFileName: String
) : Parcelable {
    STYLE_1("Style 1", "click1.mp3", "click_accent1.mp3", "click1_sub.mp3"),
    STYLE_2("Style 2", "click2.mp3", "click_accent2.mp3", "click2_sub.mp3");

    companion object {
        fun fromString(value: String): ClickStyle {
            return entries.find { it.name == value } ?: STYLE_1
        }
    }
}

/**
 * Metronome state data class
 */
@Parcelize
data class MetronomeState(
    val bpm: Int = 120,
    val isPlaying: Boolean = false,
    val beatsPerMeasure: Int = 4,
    val currentBeat: Int = 0,
    val currentSubdivision: Int = 0,
    val subdivision: Subdivision = Subdivision.QUARTER,
    val clickStyle: ClickStyle = ClickStyle.STYLE_1,
    val volume: Float = 0.8f,
    val accentFirstBeat: Boolean = true,
    val subdivisionVolume: Float = 0.5f
) : Parcelable {
    
    val beatsPerMinuteText: String
        get() = "$bpm BPM"

    val intervalMs: Long
        get() = (60_000L / bpm)

    val subdivisionIntervalMs: Long
        get() = intervalMs / subdivision.clicksPerBeat

    companion object {
        const val MIN_BPM = 20
        const val MAX_BPM = 300
        const val DEFAULT_BPM = 120
    }
}
