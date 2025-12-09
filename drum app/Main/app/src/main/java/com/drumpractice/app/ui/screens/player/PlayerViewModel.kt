package com.drumpractice.app.ui.screens.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drumpractice.app.audio.AudioPlayer
import com.drumpractice.app.audio.BpmDetector
import com.drumpractice.app.audio.MetronomeEngine
import com.drumpractice.app.data.model.MetronomeState
import com.drumpractice.app.data.model.PlayerState
import com.drumpractice.app.data.model.Song
import com.drumpractice.app.data.model.Subdivision
import com.drumpractice.app.data.repository.SettingsRepository
import com.drumpractice.app.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
    private val audioPlayer: AudioPlayer,
    private val metronomeEngine: MetronomeEngine,
    private val bpmDetector: BpmDetector
) : ViewModel() {

    private val songId: Long = savedStateHandle.get<Long>("songId") ?: 0L

    private val _song = MutableStateFlow<Song?>(null)
    val song: StateFlow<Song?> = _song

    val playerState: StateFlow<PlayerState> = audioPlayer.playerState
    val metronomeState: StateFlow<MetronomeState> = metronomeEngine.state
    val bpmDetectionState = bpmDetector.state

    private val _isMetronomeEnabled = MutableStateFlow(true)
    val isMetronomeEnabled: StateFlow<Boolean> = _isMetronomeEnabled

    init {
        audioPlayer.initialize()
        loadSong()
    }

    private fun loadSong() {
        viewModelScope.launch {
            songRepository.getSongByIdOnce(songId)?.let { song ->
                _song.value = song
                audioPlayer.loadSong(song)
                
                // Set BPM from song if available
                song.effectiveBpm?.let { bpm ->
                    metronomeEngine.setBpm(bpm)
                }

                // Update last played
                songRepository.updateLastPlayed(songId)
            }
        }
    }

    // Player controls
    fun play() = audioPlayer.play()
    fun pause() = audioPlayer.pause()
    fun stop() = audioPlayer.stop()
    fun togglePlayPause() = audioPlayer.togglePlayPause()
    fun seekTo(position: Long) = audioPlayer.seekTo(position)
    fun seekForward() = audioPlayer.seekForward()
    fun seekBackward() = audioPlayer.seekBackward()
    fun setVolume(volume: Float) = audioPlayer.setVolume(volume)
    fun setPlaybackSpeed(speed: Float) = audioPlayer.setPlaybackSpeed(speed)

    // Metronome controls
    fun toggleMetronome() {
        if (metronomeState.value.isPlaying) {
            metronomeEngine.stop()
        } else {
            metronomeEngine.start()
        }
    }

    fun setMetronomeEnabled(enabled: Boolean) {
        _isMetronomeEnabled.value = enabled
        if (!enabled) {
            metronomeEngine.stop()
        }
    }

    fun setBpm(bpm: Int) = metronomeEngine.setBpm(bpm)
    fun setSubdivision(subdivision: Subdivision) = metronomeEngine.setSubdivision(subdivision)
    fun setMetronomeVolume(volume: Float) = metronomeEngine.setVolume(volume)
    fun tapTempo() = metronomeEngine.tapTempo()

    // BPM Detection
    fun detectBpm() {
        _song.value?.let { song ->
            bpmDetector.detectBpm(song.filePath)
        }
    }

    fun applyDetectedBpm() {
        bpmDetector.state.value.result?.let { result ->
            setBpm(result.bpm)
            viewModelScope.launch {
                songRepository.updateDetectedBpm(songId, result.bpm)
                _song.value = _song.value?.copy(detectedBpm = result.bpm)
            }
        }
    }

    fun setManualBpm(bpm: Int?) {
        viewModelScope.launch {
            songRepository.updateManualBpm(songId, bpm)
            _song.value = _song.value?.copy(manualBpm = bpm)
            bpm?.let { setBpm(it) }
        }
    }

    // Sync metronome with playback
    fun syncMetronomeWithPlayback() {
        if (_isMetronomeEnabled.value && playerState.value.isPlaying) {
            if (!metronomeState.value.isPlaying) {
                metronomeEngine.start()
            }
        } else {
            metronomeEngine.stop()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        metronomeEngine.stop()
        bpmDetector.cancel()
    }
}
