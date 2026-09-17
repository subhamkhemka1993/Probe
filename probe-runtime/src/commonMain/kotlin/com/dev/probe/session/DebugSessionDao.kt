package com.dev.probe.session

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DebugSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: DebugSessionEntity)

    @Query("SELECT * FROM debug_sessions WHERE role = :role LIMIT 1")
    suspend fun getByRole(role: String): DebugSessionEntity?

    @Query("SELECT * FROM debug_sessions ORDER BY startedAtMillis DESC")
    suspend fun getAll(): List<DebugSessionEntity>

    @Query("SELECT * FROM debug_sessions ORDER BY startedAtMillis DESC")
    fun observeAll(): Flow<List<DebugSessionEntity>>

    @Query("UPDATE debug_sessions SET role = :role, endedAtMillis = :endedAtMillis WHERE id = :id")
    suspend fun updateRole(
        id: String,
        role: String,
        endedAtMillis: Long?,
    )

    @Query("DELETE FROM debug_sessions WHERE role NOT IN ('current', 'previous')")
    suspend fun deleteOrphans()

    @Query("DELETE FROM debug_sessions WHERE id NOT IN (:sessionIds)")
    suspend fun deleteSessionsNotIn(sessionIds: List<String>)

    @Query("DELETE FROM debug_sessions")
    suspend fun deleteAll()
}
