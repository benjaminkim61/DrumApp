package com.drumpractice.app.ui.screens.recording

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drumpractice.app.audio.AudioPlayer
import com.drumpractice.app.data.model.PostRecordingState
import com.drumpractice.app.data.model.Recording
import com.drumpractice.app.data.model.Song
import com.drumpractice.app.data.repository.RecordingRepository
import com.drumpractice.app.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostRecordingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingRepository: RecordingRepository,
    private val songRepository: SongRepository,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val recordingId: Long = savedStateHandle.get<Long>("recordingId") ?: 0L

    private val _recording = MutableStateFlow<Recording?>(null)
    val recording: StateFlow<Recording?> = _recording

    private val _backingSong = MutableStateFlow<Song?>(null)
    val backingSong: StateFlow<Song?> = _backingSong

    private val _state = MutableStateFlow(PostRecordingState())
    val state: StateFlow<PostRecordingState> = _state

    val playerState = audioPlayer.playerState

    init {
        audioPlayer.initialize()
        loadRecording()
    }

    private fun loadRecording() {
        viewModelScope.launch {
            recordingRepository.getRecordingByIdOnce(recordingId)?.let { rec ->
                _recording.value = rec
                _state.value = PostRecordingState(
                    recording = rec,
                    audioDelayMs = rec.audioDelay,
                    recordingVolume = rec.recordingVolume,
                    backtrackVolume = rec.backtrackVolume,
                    metronomeVolume = rec.metronomeVolume
                )

                // Load backing song if exists
                rec.songId?.let { songId ->
                    songRepository.getSongByIdOnce(songId)?.let { song ->
                        _backingSong.value = song
                    }
                }

                // Load recording into player
                audioPlayer.loadUri(Uri.parse("file://${rec.filePath}"), rec.duration)
            }
        }
    }

    fun setName(name: String) {
        _recording.value?.let { rec ->
            _recording.value = rec.copy(name = name)
            _state.value = _state.value.copy(hasUnsavedChanges = true)
        }
    }

    fun setAudioDelay(delayMs: Long) {
        _state.value = _state.value.copy(
            audioDelayMs = delayMs,
            hasUnsavedChanges = true
        )
    }

    fun setRecordingVolume(volume: Float) {
        _state.value = _state.value.copy(
            recordingVolume = volume.coerceIn(0f, 1f),
            hasUnsavedChanges = true
        )
        // Apply to current playback
        audioPlayer.setVolume(volume)
    }

    fun setBacktrackVolume(volume: Float) {
        _state.value = _state.value.copy(
            backtrackVolume = volume.coerceIn(0f, 1f),
            hasUnsavedChanges = true
        )
    }

    fun setMetronomeVolume(volume: Float) {
        _state.value = _state.value.copy(
            metronomeVolume = volume.coerceIn(0f, 1f),
            hasUnsavedChanges = true
        )
    }

    // Playback controls
    fun play() = audioPlayer.play()
    fun pause() = audioPlayer.pause()
    fun togglePlayPause() = audioPlayer.togglePlayPause()
    fun seekTo(position: Long) = audioPlayer.seekTo(position)

    fun saveChanges(onComplete: () -> Unit) {
        viewModelScope.launch {
            _recording.value?.let { rec ->
                val updatedRecording = rec.copy(
                    audioDelay = _state.value.audioDelayMs,
                    recordingVolume = _state.value.recordingVolume,
                    backtrackVolume = _state.value.backtrackVolume,
                    metronomeVolume = _state.value.metronomeVolume
                )
                recordingRepository.updateRecording(updatedRecording)
                _state.value = _state.value.copy(hasUnsavedChanges = false)
                onComplete()
            }
        }
    }

    fun deleteRecording(onComplete: () -> Unit) {
        viewModelScope.launch {
            _recording.value?.let { rec ->
                // Delete file
                try {
                    java.io.File(rec.filePath).delete()
                    rec.thumbnailPath?.let { java.io.File(it).delete() }
                } catch (e: Exception) {
                    // Ignore
                }
                recordingRepository.deleteRecording(rec)
                onComplete()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
