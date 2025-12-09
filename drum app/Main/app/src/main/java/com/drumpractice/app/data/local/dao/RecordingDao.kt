package com.drumpractice.app.data.local.dao

import androidx.room.*
import com.drumpractice.app.data.local.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY dateCreated DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: Long): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE id = :id")
    fun getRecordingByIdFlow(id: Long): Flow<RecordingEntity?>

    @Query("SELECT * FROM recordings WHERE songId = :songId ORDER BY dateCreated DESC")
    fun getRecordingsForSong(songId: Long): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE isVideo = :isVideo ORDER BY dateCreated DESC")
    fun getRecordingsByType(isVideo: Boolean): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings ORDER BY dateCreated DESC LIMIT :limit")
    fun getRecentRecordings(limit: Int = 10): Flow<List<RecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: RecordingEntity): Long

    @Update
    suspend fun update(recording: RecordingEntity)

    @Delete
    suspend fun delete(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE recordings SET audioDelay = :delay WHERE id = :id")
    suspend fun updateAudioDelay(id: Long, delay: Long)

    @Query("UPDATE recordings SET recordingVolume = :volume WHERE id = :id")
    suspend fun updateRecordingVolume(id: Long, volume: Float)

    @Query("UPDATE recordings SET backtrackVolume = :volume WHERE id = :id")
    suspend fun updateBacktrackVolume(id: Long, volume: Float)

    @Query("UPDATE recordings SET metronomeVolume = :volume WHERE id = :id")
    suspend fun updateMetronomeVolume(id: Long, volume: Float)

    @Query("UPDATE recordings SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String)

    @Query("SELECT COUNT(*) FROM recordings")
    suspend fun getRecordingCount(): Int

    @Query("SELECT SUM(fileSize) FROM recordings")
    suspend fun getTotalStorageUsed(): Long?
}
