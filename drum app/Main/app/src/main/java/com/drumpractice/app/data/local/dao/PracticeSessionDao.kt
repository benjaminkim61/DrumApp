package com.drumpractice.app.data.local.dao

import androidx.room.*
import com.drumpractice.app.data.local.entity.PracticeSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeSessionDao {
    @Query("SELECT * FROM practice_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): PracticeSessionEntity?

    @Query("SELECT * FROM practice_sessions WHERE songId = :songId ORDER BY date DESC")
    fun getSessionsForSong(songId: Long): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_sessions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getSessionsInRange(startDate: Long, endDate: Long): Flow<List<PracticeSessionEntity>>

    @Query("SELECT SUM(durationSeconds) FROM practice_sessions WHERE date >= :startDate")
    suspend fun getTotalPracticeTime(startDate: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: PracticeSessionEntity): Long

    @Update
    suspend fun update(session: PracticeSessionEntity)

    @Delete
    suspend fun delete(session: PracticeSessionEntity)

    @Query("DELETE FROM practice_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
