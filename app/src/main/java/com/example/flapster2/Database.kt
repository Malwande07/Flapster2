package com.example.flapster2

import androidx.room.*
import android.content.Context
import kotlinx.coroutines.flow.Flow

// Room Entity
@Entity(tableName = "high_scores")
data class HighScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val playerName: String,
    val score: Int,
    val difficulty: String,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

// Room DAO
@Dao
interface HighScoreDao {
    @Query("SELECT * FROM high_scores ORDER BY score DESC LIMIT 10")
    fun getTopScores(): Flow<List<HighScoreEntity>>

    @Query("SELECT * FROM high_scores WHERE synced = 0")
    suspend fun getUnsyncedScores(): List<HighScoreEntity>

    @Insert
    suspend fun insertScore(score: HighScoreEntity)

    @Update
    suspend fun updateScore(score: HighScoreEntity)

    @Query("SELECT MAX(score) FROM high_scores")
    suspend fun getHighestScore(): Int?

    @Query("DELETE FROM high_scores")
    suspend fun clearAll()
}

// Room Database
@Database(entities = [HighScoreEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun highScoreDao(): HighScoreDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flapster2_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
