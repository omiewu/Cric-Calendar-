package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val seriesName: String,
    val matchNumber: String,
    val formatCode: String,
    val statusCode: String,
    val teamA: String,
    val teamB: String,
    val teamACode: String,
    val teamBCode: String,
    val teamAScore: String?,
    val teamBScore: String?,
    val resultSummary: String?,
    val venue: String,
    val city: String,
    val startTimeUtcIso: String,
    val startTimeEpochMs: Long,
    val lastUpdatedMs: Long
)

@Entity(tableName = "match_reminders")
data class MatchReminderEntity(
    @PrimaryKey val matchId: String,
    val enabled: Boolean,
    val notify12h: Boolean,
    val notify2h: Boolean,
    val notify5m: Boolean,
    val updatedAtMs: Long
)

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY startTimeEpochMs ASC")
    fun observeAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches ORDER BY startTimeEpochMs ASC")
    suspend fun getAllMatchesOnce(): List<MatchEntity>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchById(matchId: String): MatchEntity?

    @Query("SELECT COUNT(*) FROM matches")
    suspend fun countMatches(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMatches(matches: List<MatchEntity>)

    @Query("DELETE FROM matches")
    suspend fun clearAllMatches()
}

@Dao
interface MatchReminderDao {
    @Query("SELECT * FROM match_reminders")
    fun observeAllReminders(): Flow<List<MatchReminderEntity>>

    @Query("SELECT * FROM match_reminders WHERE enabled = 1")
    suspend fun getEnabledRemindersOnce(): List<MatchReminderEntity>

    @Query("SELECT * FROM match_reminders WHERE matchId = :matchId LIMIT 1")
    suspend fun getReminderByMatchId(matchId: String): MatchReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: MatchReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminders(reminders: List<MatchReminderEntity>)
}

@Database(
    entities = [MatchEntity::class, MatchReminderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CricPulseDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun matchReminderDao(): MatchReminderDao

    companion object {
        @Volatile
        private var INSTANCE: CricPulseDatabase? = null

        fun getInstance(context: Context): CricPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CricPulseDatabase::class.java,
                    "cricpulse_database.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
