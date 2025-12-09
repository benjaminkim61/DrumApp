package com.drumpractice.app.data.repository

import com.drumpractice.app.data.local.dao.SongDao
import com.drumpractice.app.data.mapper.toEntity
import com.drumpractice.app.data.mapper.toModel
import com.drumpractice.app.data.mapper.toSongModels
import com.drumpractice.app.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao
) {
    fun getAllSongs(): Flow<List<Song>> = 
        songDao.getAllSongs().map { it.toSongModels() }

    fun getSongById(id: Long): Flow<Song?> = 
        songDao.getSongByIdFlow(id).map { it?.toModel() }

    suspend fun getSongByIdOnce(id: Long): Song? = 
        songDao.getSongById(id)?.toModel()

    fun getRecentlyPlayed(limit: Int = 10): Flow<List<Song>> = 
        songDao.getRecentlyPlayed(limit).map { it.toSongModels() }

    fun searchSongs(query: String): Flow<List<Song>> = 
        songDao.searchSongs(query).map { it.toSongModels() }

    suspend fun addSong(song: Song): Long = 
        songDao.insert(song.toEntity())

    suspend fun updateSong(song: Song) = 
        songDao.update(song.toEntity())

    suspend fun deleteSong(song: Song) = 
        songDao.delete(song.toEntity())

    suspend fun deleteSongById(id: Long) = 
        songDao.deleteById(id)

    suspend fun updateLastPlayed(id: Long) = 
        songDao.updateLastPlayed(id)

    suspend fun updateDetectedBpm(id: Long, bpm: Int) = 
        songDao.updateDetectedBpm(id, bpm)

    suspend fun updateManualBpm(id: Long, bpm: Int?) = 
        songDao.updateManualBpm(id, bpm)

    suspend fun getSongCount(): Int = 
        songDao.getSongCount()
}
