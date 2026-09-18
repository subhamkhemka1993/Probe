package com.dev.probe.dbinspector

import androidx.room.useWriterConnection
import com.dev.probe.db.NetworkCallEntity
import com.dev.probe.network.createTestProbeDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class SqliteRowReaderTest {
    private fun sampleCall(id: String) = NetworkCallEntity(
        id = id,
        timestampMillis = 1_000L,
        method = "GET",
        url = "https://api.example.com/test",
        host = "api.example.com",
        path = "/test",
        query = null,
        requestHeadersJson = "{}",
        requestBody = null,
        responseStatus = 200,
        responseHeadersJson = "{}",
        responseBody = "ok",
        durationMs = 42L,
        error = null,
        isComplete = true,
        sessionId = "session-1",
    )

    @Test
    fun listTablesIncludesTheDatabasesOwnTables() = runTest {
        val database = createTestProbeDatabase()

        val tables = SqliteRowReader.listTables(database)

        assertTrue("network_calls" in tables)
        assertTrue("debug_sessions" in tables)
    }

    @Test
    fun listColumnsReturnsTheEntitysColumnNames() = runTest {
        val database = createTestProbeDatabase()

        val columns = SqliteRowReader.listColumns(database, "network_calls")

        assertTrue("id" in columns)
        assertTrue("method" in columns)
        assertTrue("sessionId" in columns)
    }

    @Test
    fun readRowsReturnsInsertedDataAsText() = runTest {
        val database = createTestProbeDatabase()
        database.networkCallDao().insert(sampleCall("call-1"))

        val rows = SqliteRowReader.readRows(database, "network_calls", limit = 10, offset = 0)

        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("call-1", row["id"])
        assertEquals("GET", row["method"])
        assertEquals("200", row["responseStatus"])
        assertEquals(null, row["query"])
    }

    @Test
    fun readRowsRespectsLimitAndOffset() = runTest {
        val database = createTestProbeDatabase()
        database.networkCallDao().insert(sampleCall("call-1"))
        database.networkCallDao().insert(sampleCall("call-2"))

        val firstPage = SqliteRowReader.readRows(database, "network_calls", limit = 1, offset = 0)
        val secondPage = SqliteRowReader.readRows(database, "network_calls", limit = 1, offset = 1)

        assertEquals(1, firstPage.size)
        assertEquals(1, secondPage.size)
        assertTrue(firstPage.single()["id"] != secondPage.single()["id"])
    }

    @Test
    fun readRowsRendersBlobAndFloatColumnsAndNullsCorrectly() = runTest {
        val database = createTestProbeDatabase()
        database.useWriterConnection { transactor ->
            transactor.usePrepared("CREATE TABLE blob_test (id INTEGER PRIMARY KEY, payload BLOB, ratio REAL, label TEXT)") {
                it.step()
            }
            transactor.usePrepared("INSERT INTO blob_test (id, payload, ratio, label) VALUES (1, ?, 3.5, NULL)") { statement ->
                statement.bindBlob(1, byteArrayOf(1, 2, 3, 4, 5))
                statement.step()
            }
        }

        val row = SqliteRowReader.readRows(database, "blob_test", limit = 10, offset = 0).single()

        assertEquals("[blob 5 bytes]", row["payload"])
        assertEquals("3.5", row["ratio"])
        assertNull(row["label"])
    }
}
