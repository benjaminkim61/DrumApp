package com.drumpractice.app.data.repository

import com.drumpractice.app.data.local.dao.RecordingDao
import com.drumpractice.app.data.mapper.toEntity
import com.drumpractice.app.data.mapper.toModel
import com.drumpractice.app.data.mapper.toRecordingModels
import com.drumpractice.app.data.model.Recording
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    private val recordingDao: RecordingDao
) {
    fun getAllRecordings(): Flow<List<Recording>> = 
        recordingDao.getAllRecordings().map { it.toRecordingModels() }

    fun getRecordingById(id: Long): Flow<Recording?> = 
        recordingDao.getRecordingByIdFlow(id).map { it?.toModel() }

    suspend fun getRecordingByIdOnce(id: Long): Recording? = 
        recordingDao.getRecordingById(id)?.toModel()

    fun getRecordingsForSong(songId: Long): Flow<List<Recording>> = 
        recordingDao.getRecordingsForSong(songId).map { it.toRecordingModels() }

    fun getVideoRecordings(): Flow<List<Recording>> = 
        recordingDao.getRecordingsByType(isVideo = true).map { it.toRecordingModels() }

    fun getAudioRecordings(): Flow<List<Recording>> = 
        recordingDao.getRecordingsByType(isVideo = false).map { it.toRecordingModels() }

    fun getRecentRecordings(limit: Int = 10): Flow<List<Recording>> = 
        recordingDao.getRecentRecordings(limit).map { it.toRecordingModels() }

    suspend fun addRecording(recording: Recording): Long = 
        recordingDao.insert(recording.toEntity())

    suspend fun updateRecording(recording: Recording) = 
        recordingDao.update(recording.toEntity())

    suspend fun deleteRecording(recording: Recording) = 
        recordingDao.delete(recording.toEntity())

    suspend fun deleteRecordingById(id: Long) = 
        recordingDao.deleteById(id)

    suspend fun updateAudioDelay(id: Long, delay: Long) = 
        recordingDao.updateAudioDelay(id, delay)

    suspend fun updateRecordingVolume(id: Long, volume: Float) = 
        recordingDao.updateRecordingVolume(id, volume)

    suspend fun updateBacktrackVolume(id: Long, volume: Float) = 
        recordingDao.updateBacktrackVolume(id, volume)

    suspend fun updateMetronomeVolume(id: Long, volume: Float) = 
        recordingDao.updateMetronomeVolume(id, volume)

    suspend fun updateName(id: Long, name: String) = 
        recordingDao.updateName(id, name)

    suspend fun getRecordingCount(): Int = 
        recordingDao.getRecordingCount()

    suspend fun getTotalStorageUsed(): Long = 
        recordingDao.getTotalStorageUsed() ?: 0L
}
