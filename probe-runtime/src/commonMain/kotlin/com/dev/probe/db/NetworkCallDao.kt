package com.dev.probe.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface NetworkCallDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: NetworkCallEntity)

    @Query("SELECT * FROM network_calls WHERE id = :id")
    suspend fun getById(id: String): NetworkCallEntity?

    @Query(
        """
        SELECT * FROM network_calls
        WHERE sessionId = :sessionId
            AND (:query = '' OR url LIKE '%' || :query || '%'
                OR path LIKE '%' || :query || '%'
                OR method LIKE '%' || :query || '%'
                OR CAST(responseStatus AS TEXT) LIKE '%' || :query || '%')
        ORDER BY timestampMillis DESC
        LIMIT :limit
        """,
    )
    fun observeSearch(sessionId: String, query: String, limit: Int): Flow<List<NetworkCallEntity>>

    @Query("DELETE FROM network_calls")
    suspend fun clearAll()

    @Query("DELETE FROM network_calls WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)

    @Query("DELETE FROM network_calls WHERE sessionId NOT IN (:sessionIds)")
    suspend fun deleteSessionsNotIn(sessionIds: List<String>)

    @Query(
        """
        DELETE FROM network_calls WHERE sessionId = :sessionId AND id IN (
            SELECT id FROM network_calls WHERE sessionId = :sessionId
            ORDER BY timestampMillis ASC
            LIMIT MAX(0, (SELECT COUNT(*) FROM network_calls WHERE sessionId = :sessionId) - :maxEntries)
        )
        """,
    )
    suspend fun enforceCountCapForSession(sessionId: String, maxEntries: Int)
}
