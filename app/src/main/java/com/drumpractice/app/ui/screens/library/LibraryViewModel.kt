package com.drumpractice.app.ui.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drumpractice.app.data.model.Recording
import com.drumpractice.app.data.model.Song
import com.drumpractice.app.data.repository.RecordingRepository
import com.drumpractice.app.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    val songs: StateFlow<List<Song>> = songRepository.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recordings: StateFlow<List<Recording>> = recordingRepository.getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredSongs: StateFlow<List<Song>> = combine(songs, searchQuery) { songs, query ->
        if (query.isBlank()) songs
        else songs.filter { 
            it.title.contains(query, ignoreCase = true) || 
            it.artist?.contains(query, ignoreCase = true) == true 
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredRecordings: StateFlow<List<Recording>> = combine(recordings, searchQuery) { recordings, query ->
        if (query.isBlank()) recordings
        else recordings.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            // Delete file
            try {
                File(song.filePath).delete()
            } catch (e: Exception) {
                // Ignore file deletion errors
            }
            songRepository.deleteSong(song)
        }
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            // Delete file
            try {
                File(recording.filePath).delete()
                recording.thumbnailPath?.let { File(it).delete() }
            } catch (e: Exception) {
                // Ignore file deletion errors
            }
            recordingRepository.deleteRecording(recording)
        }
    }

    fun renameSong(song: Song, newName: String) {
        viewModelScope.launch {
            songRepository.updateSong(song.copy(title = newName))
        }
    }

    fun renameRecording(recording: Recording, newName: String) {
        viewModelScope.launch {
            recordingRepository.updateName(recording.id, newName)
        }
    }
}
