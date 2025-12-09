package com.drumpractice.app.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.drumpractice.app.data.model.PlayerState
import com.drumpractice.app.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio player using ExoPlayer/Media3 for robust audio playback
 * with accurate position tracking.
 */
@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionUpdateJob: Job? = null

    // Callbacks
    private var onPositionChanged: ((Long) -> Unit)? = null
    private var onPlaybackComplete: (() -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updateState()
            when (playbackState) {
                Player.STATE_ENDED -> {
                    if (_playerState.value.isLooping) {
                        seekTo(_playerState.value.loopStartMs ?: 0)
                        play()
                    } else {
                        onPlaybackComplete?.invoke()
                    }
                }
                Player.STATE_READY -> {
                    startPositionTracking()
                }
                Player.STATE_IDLE, Player.STATE_BUFFERING -> {
                    // Handle as needed
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState()
            if (isPlaying) {
                startPositionTracking()
            } else {
                stopPositionTracking()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            onError?.invoke(error.message ?: "Playback error")
            updateState()
        }
    }

    fun initialize() {
        if (exoPlayer != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                addListener(playerListener)
            }
    }

    fun setCallbacks(
        onPositionChanged: ((Long) -> Unit)? = null,
        onPlaybackComplete: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        this.onPositionChanged = onPositionChanged
        this.onPlaybackComplete = onPlaybackComplete
        this.onError = onError
    }

    fun loadSong(song: Song) {
        initialize()

        val uri = if (song.filePath.startsWith("content://")) {
            Uri.parse(song.filePath)
        } else {
            Uri.parse("file://${song.filePath}")
        }

        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
        }

        _playerState.value = _playerState.value.copy(
            currentSong = song,
            duration = song.duration,
            currentPosition = 0,
            isPlaying = false,
            isPaused = false
        )
    }

    fun loadUri(uri: Uri, duration: Long = 0) {
        initialize()

        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
        }

        _playerState.value = _playerState.value.copy(
            currentSong = null,
            duration = duration,
            currentPosition = 0,
            isPlaying = false,
            isPaused = false
        )
    }

    fun play() {
        exoPlayer?.play()
        updateState()
    }

    fun pause() {
        exoPlayer?.pause()
        _playerState.value = _playerState.value.copy(isPaused = true)
        updateState()
    }

    fun stop() {
        exoPlayer?.apply {
            stop()
            seekTo(0)
        }
        _playerState.value = _playerState.value.copy(
            isPlaying = false,
            isPaused = false,
            currentPosition = 0
        )
    }

    fun togglePlayPause() {
        if (_playerState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs.coerceAtLeast(0))
        _playerState.value = _playerState.value.copy(currentPosition = positionMs)
    }

    fun seekForward(deltaMs: Long = 10000) {
        val newPosition = (_playerState.value.currentPosition + deltaMs)
            .coerceAtMost(_playerState.value.duration)
        seekTo(newPosition)
    }

    fun seekBackward(deltaMs: Long = 10000) {
        val newPosition = (_playerState.value.currentPosition - deltaMs)
            .coerceAtLeast(0)
        seekTo(newPosition)
    }

    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = clampedVolume
        _playerState.value = _playerState.value.copy(volume = clampedVolume)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(0.25f, 2.0f)
        exoPlayer?.setPlaybackSpeed(clampedSpeed)
        _playerState.value = _playerState.value.copy(playbackSpeed = clampedSpeed)
    }

    fun setLooping(looping: Boolean, startMs: Long? = null, endMs: Long? = null) {
        _playerState.value = _playerState.value.copy(
            isLooping = looping,
            loopStartMs = startMs,
            loopEndMs = endMs
        )

        if (looping && endMs != null) {
            // Custom loop implementation - check position in tracking loop
        } else {
            exoPlayer?.repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
    }

    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0

    fun getDuration(): Long = exoPlayer?.duration ?: 0

    private fun updateState() {
        exoPlayer?.let { player ->
            val isPlaying = player.isPlaying
            val position = player.currentPosition
            val duration = if (player.duration > 0) player.duration else _playerState.value.duration

            _playerState.value = _playerState.value.copy(
                isPlaying = isPlaying,
                currentPosition = position,
                duration = duration
            )
        }
    }

    private fun startPositionTracking() {
        stopPositionTracking()
        positionUpdateJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    val position = player.currentPosition
                    _playerState.value = _playerState.value.copy(currentPosition = position)
                    onPositionChanged?.invoke(position)

                    // Check for custom loop end
                    val loopEnd = _playerState.value.loopEndMs
                    if (_playerState.value.isLooping && loopEnd != null && position >= loopEnd) {
                        seekTo(_playerState.value.loopStartMs ?: 0)
                    }
                }
                delay(50) // Update every 50ms for smooth progress
            }
        }
    }

    private fun stopPositionTracking() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun release() {
        stopPositionTracking()
        scope.cancel()
        exoPlayer?.apply {
            removeListener(playerListener)
            release()
        }
        exoPlayer = null
        _playerState.value = PlayerState()
    }
}
