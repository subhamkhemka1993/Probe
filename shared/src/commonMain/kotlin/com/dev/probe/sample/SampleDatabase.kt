package com.dev.probe.sample

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dev.probe.api.ProbePlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

internal const val SAMPLE_DB_NAME = "probe_sample.db"

@Entity(tableName = "sample_notes")
data class SampleNoteEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val text: String, val createdAtMillis: Long)

@Dao
interface SampleNoteDao {
    @Insert
    suspend fun insert(note: SampleNoteEntity)

    @Query("SELECT * FROM sample_notes ORDER BY id DESC")
    fun observeAll(): Flow<List<SampleNoteEntity>>
}

@Database(entities = [SampleNoteEntity::class], version = 1)
@ConstructedBy(SampleDatabaseConstructor::class)
abstract class SampleDatabase : RoomDatabase() {
    abstract fun sampleNoteDao(): SampleNoteDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SampleDatabaseConstructor : RoomDatabaseConstructor<SampleDatabase> {
    override fun initialize(): SampleDatabase
}

internal expect fun sampleDatabaseBuilder(platform: ProbePlatformContext): RoomDatabase.Builder<SampleDatabase>

/** Builds and opens the sample app's own tiny Room database, ready to register with Probe. */
fun createSampleDatabase(platform: ProbePlatformContext): SampleDatabase = sampleDatabaseBuilder(platform)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
