@file:OptIn(ExperimentalTime::class)

package com.dev.probe.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.dev.probe.session.DebugSessionDao
import com.dev.probe.session.DebugSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal const val Z_DEBUG_DB_NAME = "probe_network.db"

@Database(
    entities = [NetworkCallEntity::class, DebugSessionEntity::class],
    version = 2,
)
@ConstructedBy(ProbeDatabaseConstructor::class)
internal abstract class ProbeDatabase : RoomDatabase() {
    abstract fun networkCallDao(): NetworkCallDao

    abstract fun debugSessionDao(): DebugSessionDao

    internal companion object {
        val MIGRATION_1_2: Migration =
            object : Migration(startVersion = 1, endVersion = 2) {
                override fun migrate(connection: SQLiteConnection) {
                    val nowMillis = Clock.System.now().toEpochMilliseconds()
                    val currentSessionId = "session-$nowMillis"

                    connection.execSQL(
                        "CREATE TABLE IF NOT EXISTS `debug_sessions` (`id` TEXT NOT NULL, " +
                            "`label` TEXT NOT NULL, `startedAtMillis` INTEGER NOT NULL, " +
                            "`endedAtMillis` INTEGER, `role` TEXT NOT NULL, PRIMARY KEY(`id`))",
                    )
                    connection.execSQL(
                        "ALTER TABLE `network_calls` ADD COLUMN `sessionId` TEXT NOT NULL DEFAULT 'legacy'",
                    )
                    connection.execSQL(
                        "INSERT INTO `debug_sessions` (`id`, `label`, `startedAtMillis`, `endedAtMillis`, `role`) " +
                            "VALUES ('legacy', 'Legacy session', $nowMillis, $nowMillis, 'previous')",
                    )
                    connection.execSQL(
                        "INSERT INTO `debug_sessions` (`id`, `label`, `startedAtMillis`, `endedAtMillis`, `role`) " +
                            "VALUES ('$currentSessionId', 'Session 1', $nowMillis, NULL, 'current')",
                    )
                    connection.execSQL("UPDATE `network_calls` SET `sessionId` = 'legacy'")
                    connection.execSQL(
                        "CREATE INDEX IF NOT EXISTS `idx_network_calls_session` " +
                            "ON `network_calls` (`sessionId`)",
                    )
                }
            }
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect object ProbeDatabaseConstructor : RoomDatabaseConstructor<ProbeDatabase> {
    override fun initialize(): ProbeDatabase
}

internal fun getProbeDatabase(builder: RoomDatabase.Builder<ProbeDatabase>): ProbeDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(ProbeDatabase.MIGRATION_1_2)
        .build()
