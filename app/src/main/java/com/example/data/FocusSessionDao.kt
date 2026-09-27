package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY timestampMillis DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY timestampMillis DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 50): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE timestampMillis >= :startTime AND timestampMillis <= :endTime ORDER BY timestampMillis DESC")
    fun getSessionsBetween(startTime: Long, endTime: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE taskId = :taskId ORDER BY timestampMillis DESC")
    fun getSessionsForTask(taskId: Long): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Delete
    suspend fun deleteSession(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions")
    suspend fun getAllSessionsDirect(): List<FocusSessionEntity>
}
