package com.drumpractice.app.ui.screens.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drumpractice.app.audio.AudioPlayer
import com.drumpractice.app.audio.MetronomeEngine
import com.drumpractice.app.data.model.MetronomeState
import com.drumpractice.app.data.model.PlayerState
import com.drumpractice.app.data.model.Song
import com.drumpractice.app.data.model.Subdivision
import com.drumpractice.app.data.repository.SettingsRepository
import com.drumpractice.app.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
    private val metronomeEngine: MetronomeEngine,
    private val audioPlayer: AudioPlayer,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val metronomeState: StateFlow<MetronomeState> = metronomeEngine.state
    val playerState: StateFlow<PlayerState> = audioPlayer.playerState
    val recentSongs: StateFlow<List<Song>> = songRepository.getRecentlyPlayed(5)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val settings = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        audioPlayer.initialize()
        
        // Load default settings when available
        viewModelScope.launch {
            settingsRepository.settings.collect { appSettings ->
                metronomeEngine.setBpm(appSettings.defaultBpm)
                metronomeEngine.setSubdivision(appSettings.defaultSubdivision)
                metronomeEngine.setVolume(appSettings.defaultMetronomeVolume)
                metronomeEngine.setSubdivisionVolume(appSettings.defaultSubdivisionVolume)
                metronomeEngine.setAccentFirstBeat(appSettings.accentFirstBeat)
                metronomeEngine.loadClickSounds(appSettings.clickStyle)
                metronomeEngine.setLatencyOffset(appSettings.latencyOffsetMs.toLong())
            }
        }
    }

    // Metronome controls
    fun toggleMetronome() = metronomeEngine.toggle()
    fun startMetronome() = metronomeEngine.start()
    fun stopMetronome() = metronomeEngine.stop()
    fun setBpm(bpm: Int) = metronomeEngine.setBpm(bpm)
    fun setSubdivision(subdivision: Subdivision) = metronomeEngine.setSubdivision(subdivision)
    fun setBeatsPerMeasure(beats: Int) = metronomeEngine.setBeatsPerMeasure(beats)
    fun setMetronomeVolume(volume: Float) = metronomeEngine.setVolume(volume)
    fun setSubdivisionVolume(volume: Float) = metronomeEngine.setSubdivisionVolume(volume)
    fun tapTempo(): Int = metronomeEngine.tapTempo()
    fun setAccentFirstBeat(accent: Boolean) = metronomeEngine.setAccentFirstBeat(accent)

    // Player controls
    fun playSong(song: Song) {
        audioPlayer.loadSong(song)
        audioPlayer.play()
        viewModelScope.launch {
            songRepository.updateLastPlayed(song.id)
        }
    }

    fun togglePlayPause() = audioPlayer.togglePlayPause()
    fun stop() = audioPlayer.stop()
    fun seekTo(position: Long) = audioPlayer.seekTo(position)
    fun setPlayerVolume(volume: Float) = audioPlayer.setVolume(volume)

    // Import song from URI
    fun importSong(uri: Uri, onComplete: (Song?) -> Unit) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                
                // Get file name from URI
                var fileName = "imported_song.mp3"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                }

                // Copy file to app storage
                val songsDir = File(context.filesDir, "songs")
                songsDir.mkdirs()
                val destFile = File(songsDir, fileName)

                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Get duration using MediaMetadataRetriever
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(destFile.absolutePath)
                val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationStr?.toLongOrNull() ?: 0L
                val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: fileName.substringBeforeLast(".")
                retriever.release()

                val song = Song(
                    title = title,
                    artist = artist,
                    filePath = destFile.absolutePath,
                    duration = duration,
                    fileSize = destFile.length()
                )

                val id = songRepository.addSong(song)
                onComplete(song.copy(id = id))

            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        metronomeEngine.stop()
    }
}
