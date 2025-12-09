package com.drumpractice.app.ui.screens.recording

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drumpractice.app.audio.AudioPlayer
import com.drumpractice.app.audio.AudioRecorder
import com.drumpractice.app.audio.MetronomeEngine
import com.drumpractice.app.audio.VideoRecorder
import com.drumpractice.app.data.model.*
import com.drumpractice.app.data.repository.RecordingRepository
import com.drumpractice.app.data.repository.SettingsRepository
import com.drumpractice.app.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val videoRecorder: VideoRecorder,
    private val metronomeEngine: MetronomeEngine,
    private val audioPlayer: AudioPlayer,
    private val recordingRepository: RecordingRepository,
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val audioRecordingStatus = audioRecorder.status
    val videoRecordingStatus = videoRecorder.status
    val metronomeState = metronomeEngine.state
    val playerState = audioPlayer.playerState
    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState

    private val _selectedSong = MutableStateFlow<Song?>(null)
    val selectedSong: StateFlow<Song?> = _selectedSong

    private val _isVideoMode = MutableStateFlow(false)
    val isVideoMode: StateFlow<Boolean> = _isVideoMode

    private val _useMetronome = MutableStateFlow(true)
    val useMetronome: StateFlow<Boolean> = _useMetronome

    private val _useBackingTrack = MutableStateFlow(false)
    val useBackingTrack: StateFlow<Boolean> = _useBackingTrack

    val songs: StateFlow<List<Song>> = songRepository.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var currentRecordingPath: String? = null

    init {
        audioPlayer.initialize()
        
        // Update recording state from audio recorder
        viewModelScope.launch {
            audioRecorder.status.collect { status ->
                if (!_isVideoMode.value) {
                    _recordingState.value = _recordingState.value.copy(
                        isRecording = status.isRecording,
                        isPaused = status.isPaused,
                        recordingDuration = status.duration,
                        audioLevel = status.audioLevel
                    )
                }
            }
        }

        // Update recording state from video recorder
        viewModelScope.launch {
            videoRecorder.status.collect { status ->
                if (_isVideoMode.value) {
                    _recordingState.value = _recordingState.value.copy(
                        isRecording = status.isRecording,
                        isPaused = status.isPaused,
                        recordingDuration = status.duration
                    )
                }
            }
        }
    }

    fun setVideoMode(enabled: Boolean) {
        _isVideoMode.value = enabled
    }

    fun setUseMetronome(enabled: Boolean) {
        _useMetronome.value = enabled
        _recordingState.value = _recordingState.value.copy(hasMetronome = enabled)
    }

    fun setUseBackingTrack(enabled: Boolean) {
        _useBackingTrack.value = enabled
        _recordingState.value = _recordingState.value.copy(hasBackingTrack = enabled)
    }

    fun selectSong(song: Song?) {
        _selectedSong.value = song
        song?.let {
            _useBackingTrack.value = true
            it.effectiveBpm?.let { bpm -> metronomeEngine.setBpm(bpm) }
            audioPlayer.loadSong(it)
        }
    }

    // Metronome controls
    fun setBpm(bpm: Int) = metronomeEngine.setBpm(bpm)
    fun setSubdivision(subdivision: Subdivision) = metronomeEngine.setSubdivision(subdivision)
    fun setMetronomeVolume(volume: Float) = metronomeEngine.setVolume(volume)
    fun tapTempo() = metronomeEngine.tapTempo()
    fun toggleMetronome() = metronomeEngine.toggle()

    fun startRecording() {
        viewModelScope.launch {
            val countdownSeconds = settings.value.countdownSeconds

            // Start countdown
            _recordingState.value = _recordingState.value.copy(
                isCountingDown = true,
                countdownValue = countdownSeconds
            )

            // Countdown loop
            for (i in countdownSeconds downTo 1) {
                _recordingState.value = _recordingState.value.copy(countdownValue = i)
                
                // Play metronome click during countdown if enabled
                if (_useMetronome.value && !metronomeEngine.state.value.isPlaying) {
                    metronomeEngine.start()
                }
                
                delay(1000)
            }

            _recordingState.value = _recordingState.value.copy(
                isCountingDown = false,
                countdownValue = 0
            )

            // Generate output path
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val extension = if (_isVideoMode.value) "mp4" else "wav"
            val recordingsDir = File(context.filesDir, "recordings")
            recordingsDir.mkdirs()
            currentRecordingPath = File(recordingsDir, "recording_$timestamp.$extension").absolutePath

            // Start actual recording
            val success = if (_isVideoMode.value) {
                videoRecorder.startRecording(currentRecordingPath!!)
            } else {
                audioRecorder.startRecording(currentRecordingPath!!)
            }

            if (success) {
                // Start backing track if enabled
                if (_useBackingTrack.value && _selectedSong.value != null) {
                    audioPlayer.play()
                }

                // Start metronome if enabled (may already be running from countdown)
                if (_useMetronome.value && !metronomeEngine.state.value.isPlaying) {
                    metronomeEngine.start()
                }
            }
        }
    }

    fun pauseRecording() {
        if (_isVideoMode.value) {
            videoRecorder.pauseRecording()
        } else {
            audioRecorder.pauseRecording()
        }
        metronomeEngine.stop()
        audioPlayer.pause()
    }

    fun resumeRecording() {
        if (_isVideoMode.value) {
            videoRecorder.resumeRecording()
        } else {
            audioRecorder.resumeRecording()
        }
        
        if (_useMetronome.value) {
            metronomeEngine.start()
        }
        if (_useBackingTrack.value) {
            audioPlayer.play()
        }
    }

    fun stopRecording(onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val filePath = if (_isVideoMode.value) {
                videoRecorder.stopRecording()
            } else {
                audioRecorder.stopRecording()
            }

            metronomeEngine.stop()
            audioPlayer.stop()

            filePath?.let { path ->
                val file = File(path)
                val recording = Recording(
                    name = "Recording ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())}",
                    filePath = path,
                    duration = _recordingState.value.recordingDuration,
                    isVideo = _isVideoMode.value,
                    songId = _selectedSong.value?.id,
                    bpmUsed = metronomeState.value.bpm,
                    subdivisionUsed = metronomeState.value.subdivision,
                    fileSize = file.length()
                )

                val id = recordingRepository.addRecording(recording)
                
                _recordingState.value = RecordingState()
                onComplete(id)
            }
        }
    }

    fun cancelRecording() {
        if (_isVideoMode.value) {
            videoRecorder.cancelRecording()
        } else {
            audioRecorder.cancelRecording()
        }
        metronomeEngine.stop()
        audioPlayer.stop()
        _recordingState.value = RecordingState()
    }

    override fun onCleared() {
        super.onCleared()
        metronomeEngine.stop()
        audioPlayer.stop()
    }
}
