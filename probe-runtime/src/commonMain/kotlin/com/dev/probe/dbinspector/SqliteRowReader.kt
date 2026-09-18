package com.dev.probe.dbinspector

import androidx.room.RoomDatabase
import androidx.room.useReaderConnection
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLITE_DATA_TEXT
import androidx.sqlite.SQLiteStatement

/**
 * Generic, schema-agnostic SQLite introspection over any registered [RoomDatabase] — no `@Dao`,
 * no `@RawQuery`, and no compile-time declaration required on the database's own class (see
 * spec §5.1). Table/column names used below are always sourced from `sqlite_master`/
 * `PRAGMA table_info` themselves, never from external input, so string-interpolating them into
 * SQL is safe in this read-only introspection context.
 */
internal object SqliteRowReader {
    suspend fun listTables(database: RoomDatabase): List<String> = database.useReaderConnection { transactor ->
        transactor.usePrepared("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name") { statement ->
            buildList {
                while (statement.step()) add(statement.getText(0))
            }
        }
    }

    suspend fun listColumns(database: RoomDatabase, table: String): List<String> = database.useReaderConnection { transactor ->
        transactor.usePrepared("PRAGMA table_info(`$table`)") { statement ->
            buildList {
                while (statement.step()) add(statement.getText(1)) // column 1 = "name"
            }
        }
    }

    suspend fun readRows(database: RoomDatabase, table: String, limit: Int, offset: Int): List<Map<String, String?>> =
        database.useReaderConnection { transactor ->
            transactor.usePrepared("SELECT * FROM `$table` LIMIT ? OFFSET ?") { statement ->
                statement.bindLong(1, limit.toLong())
                statement.bindLong(2, offset.toLong())
                val columnNames = statement.getColumnNames()
                buildList {
                    while (statement.step()) {
                        add(columnNames.indices.associate { index -> columnNames[index] to statement.readValueAsText(index) })
                    }
                }
            }
        }

    private fun SQLiteStatement.readValueAsText(index: Int): String? = when (getColumnType(index)) {
        SQLITE_DATA_NULL -> null
        SQLITE_DATA_INTEGER -> getLong(index).toString()
        SQLITE_DATA_FLOAT -> getDouble(index).toString()
        SQLITE_DATA_TEXT -> getText(index)
        SQLITE_DATA_BLOB -> "[blob ${getBlob(index).size} bytes]"
        else -> null
    }
}
