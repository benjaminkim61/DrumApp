package com.drumpractice.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drumpractice.app.data.model.*
import com.drumpractice.app.data.repository.RecordingRepository
import com.drumpractice.app.data.repository.SettingsRepository
import com.drumpractice.app.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val songRepository: SongRepository,
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _storageInfo = MutableStateFlow<StorageInfo?>(null)
    val storageInfo: StateFlow<StorageInfo?> = _storageInfo

    init {
        loadStorageInfo()
    }

    private fun loadStorageInfo() {
        viewModelScope.launch {
            val songCount = songRepository.getSongCount()
            val recordingCount = recordingRepository.getRecordingCount()
            val storageUsed = recordingRepository.getTotalStorageUsed()

            _storageInfo.value = StorageInfo(
                songCount = songCount,
                recordingCount = recordingCount,
                storageUsed = storageUsed
            )
        }
    }

    fun updateLatencyOffset(offset: Int) {
        viewModelScope.launch {
            settingsRepository.updateLatencyOffset(offset)
        }
    }

    fun updateClickStyle(style: ClickStyle) {
        viewModelScope.launch {
            settingsRepository.updateClickStyle(style)
        }
    }

    fun updateDefaultBpm(bpm: Int) {
        viewModelScope.launch {
            settingsRepository.updateDefaultBpm(bpm)
        }
    }

    fun updateDefaultSubdivision(subdivision: Subdivision) {
        viewModelScope.launch {
            settingsRepository.updateDefaultSubdivision(subdivision)
        }
    }

    fun updateCountdownSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.updateCountdownSeconds(seconds)
        }
    }

    fun updateVideoQuality(quality: VideoQuality) {
        viewModelScope.launch {
            settingsRepository.updateVideoQuality(quality)
        }
    }

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.updateTheme(theme)
        }
    }

    fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateHapticFeedback(enabled)
        }
    }

    fun updateAccentFirstBeat(accent: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAccentFirstBeat(accent)
        }
    }

    fun updateDefaultMetronomeVolume(volume: Float) {
        viewModelScope.launch {
            settingsRepository.updateDefaultMetronomeVolume(volume)
        }
    }

    fun updateRecordMetronomeAudio(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateRecordMetronomeAudio(enabled)
        }
    }
}

data class StorageInfo(
    val songCount: Int,
    val recordingCount: Int,
    val storageUsed: Long
) {
    val formattedStorage: String
        get() = when {
            storageUsed < 1024 -> "$storageUsed B"
            storageUsed < 1024 * 1024 -> "%.1f KB".format(storageUsed / 1024.0)
            storageUsed < 1024 * 1024 * 1024 -> "%.1f MB".format(storageUsed / (1024.0 * 1024.0))
            else -> "%.2f GB".format(storageUsed / (1024.0 * 1024.0 * 1024.0))
        }
}
