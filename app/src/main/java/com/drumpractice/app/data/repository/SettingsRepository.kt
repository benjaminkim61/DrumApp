package com.drumpractice.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.drumpractice.app.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Preference keys
    private object PreferenceKeys {
        val LATENCY_OFFSET = intPreferencesKey("latency_offset_ms")
        val CLICK_STYLE = stringPreferencesKey("click_style")
        val DEFAULT_BPM = intPreferencesKey("default_bpm")
        val DEFAULT_SUBDIVISION = stringPreferencesKey("default_subdivision")
        val COUNTDOWN_SECONDS = intPreferencesKey("countdown_seconds")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val AUDIO_SAMPLE_RATE = intPreferencesKey("audio_sample_rate")
        val RECORD_METRONOME_AUDIO = booleanPreferencesKey("record_metronome_audio")
        val THEME = stringPreferencesKey("theme")
        val SHOW_BPM_ON_WAVEFORM = booleanPreferencesKey("show_bpm_on_waveform")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val DEFAULT_BEATS_PER_MEASURE = intPreferencesKey("default_beats_per_measure")
        val ACCENT_FIRST_BEAT = booleanPreferencesKey("accent_first_beat")
        val DEFAULT_METRONOME_VOLUME = floatPreferencesKey("default_metronome_volume")
        val DEFAULT_SUBDIVISION_VOLUME = floatPreferencesKey("default_subdivision_volume")
    }

    val settings: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                latencyOffsetMs = preferences[PreferenceKeys.LATENCY_OFFSET] ?: 0,
                clickStyle = preferences[PreferenceKeys.CLICK_STYLE]?.let { 
                    ClickStyle.fromString(it) 
                } ?: ClickStyle.STYLE_1,
                defaultBpm = preferences[PreferenceKeys.DEFAULT_BPM] ?: 120,
                defaultSubdivision = preferences[PreferenceKeys.DEFAULT_SUBDIVISION]?.let { 
                    Subdivision.fromString(it) 
                } ?: Subdivision.QUARTER,
                countdownSeconds = preferences[PreferenceKeys.COUNTDOWN_SECONDS] ?: 4,
                videoQuality = preferences[PreferenceKeys.VIDEO_QUALITY]?.let { 
                    VideoQuality.valueOf(it) 
                } ?: VideoQuality.HD_720P,
                audioSampleRate = preferences[PreferenceKeys.AUDIO_SAMPLE_RATE] ?: 44100,
                recordMetronomeAudio = preferences[PreferenceKeys.RECORD_METRONOME_AUDIO] ?: false,
                theme = preferences[PreferenceKeys.THEME]?.let { 
                    AppTheme.valueOf(it) 
                } ?: AppTheme.SYSTEM,
                showBpmOnWaveform = preferences[PreferenceKeys.SHOW_BPM_ON_WAVEFORM] ?: true,
                hapticFeedback = preferences[PreferenceKeys.HAPTIC_FEEDBACK] ?: true,
                defaultBeatsPerMeasure = preferences[PreferenceKeys.DEFAULT_BEATS_PER_MEASURE] ?: 4,
                accentFirstBeat = preferences[PreferenceKeys.ACCENT_FIRST_BEAT] ?: true,
                defaultMetronomeVolume = preferences[PreferenceKeys.DEFAULT_METRONOME_VOLUME] ?: 0.8f,
                defaultSubdivisionVolume = preferences[PreferenceKeys.DEFAULT_SUBDIVISION_VOLUME] ?: 0.5f
            )
        }

    suspend fun updateLatencyOffset(offsetMs: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LATENCY_OFFSET] = offsetMs
        }
    }

    suspend fun updateClickStyle(style: ClickStyle) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.CLICK_STYLE] = style.name
        }
    }

    suspend fun updateDefaultBpm(bpm: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_BPM] = bpm
        }
    }

    suspend fun updateDefaultSubdivision(subdivision: Subdivision) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_SUBDIVISION] = subdivision.name
        }
    }

    suspend fun updateCountdownSeconds(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.COUNTDOWN_SECONDS] = seconds
        }
    }

    suspend fun updateVideoQuality(quality: VideoQuality) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.VIDEO_QUALITY] = quality.name
        }
    }

    suspend fun updateAudioSampleRate(sampleRate: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUDIO_SAMPLE_RATE] = sampleRate
        }
    }

    suspend fun updateRecordMetronomeAudio(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.RECORD_METRONOME_AUDIO] = enabled
        }
    }

    suspend fun updateTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME] = theme.name
        }
    }

    suspend fun updateShowBpmOnWaveform(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_BPM_ON_WAVEFORM] = show
        }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.HAPTIC_FEEDBACK] = enabled
        }
    }

    suspend fun updateDefaultBeatsPerMeasure(beats: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_BEATS_PER_MEASURE] = beats
        }
    }

    suspend fun updateAccentFirstBeat(accent: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ACCENT_FIRST_BEAT] = accent
        }
    }

    suspend fun updateDefaultMetronomeVolume(volume: Float) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_METRONOME_VOLUME] = volume
        }
    }

    suspend fun updateDefaultSubdivisionVolume(volume: Float) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_SUBDIVISION_VOLUME] = volume
        }
    }

    suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LATENCY_OFFSET] = settings.latencyOffsetMs
            preferences[PreferenceKeys.CLICK_STYLE] = settings.clickStyle.name
            preferences[PreferenceKeys.DEFAULT_BPM] = settings.defaultBpm
            preferences[PreferenceKeys.DEFAULT_SUBDIVISION] = settings.defaultSubdivision.name
            preferences[PreferenceKeys.COUNTDOWN_SECONDS] = settings.countdownSeconds
            preferences[PreferenceKeys.VIDEO_QUALITY] = settings.videoQuality.name
            preferences[PreferenceKeys.AUDIO_SAMPLE_RATE] = settings.audioSampleRate
            preferences[PreferenceKeys.RECORD_METRONOME_AUDIO] = settings.recordMetronomeAudio
            preferences[PreferenceKeys.THEME] = settings.theme.name
            preferences[PreferenceKeys.SHOW_BPM_ON_WAVEFORM] = settings.showBpmOnWaveform
            preferences[PreferenceKeys.HAPTIC_FEEDBACK] = settings.hapticFeedback
            preferences[PreferenceKeys.DEFAULT_BEATS_PER_MEASURE] = settings.defaultBeatsPerMeasure
            preferences[PreferenceKeys.ACCENT_FIRST_BEAT] = settings.accentFirstBeat
            preferences[PreferenceKeys.DEFAULT_METRONOME_VOLUME] = settings.defaultMetronomeVolume
            preferences[PreferenceKeys.DEFAULT_SUBDIVISION_VOLUME] = settings.defaultSubdivisionVolume
        }
    }
}
