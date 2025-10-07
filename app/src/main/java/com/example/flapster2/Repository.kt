package com.example.flapster2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import android.util.Log

class GameRepository(private val dao: HighScoreDao) {
    @Suppress("unused")
    val topScores: Flow<List<HighScoreEntity>> = dao.getTopScores()
    suspend fun getHighestScore(): Int {
        return withContext(Dispatchers.IO) {
            dao.getHighestScore() ?: 0
        }
    }

    suspend fun saveScore(playerName: String, score: Int, difficulty: String) {
        withContext(Dispatchers.IO) {
            // Save locally
            val entity = HighScoreEntity(
                playerName = playerName,
                score = score,
                difficulty = difficulty,
                synced = false
            )
            dao.insertScore(entity)

            // Try to sync to Supabase
            try {
                val request = HighScoreRequest(playerName, score, difficulty)
                RetrofitClient.api.submitScore(request)

                // Mark as synced
                dao.updateScore(entity.copy(synced = true))
                Log.d("GameRepository", "Score synced to Supabase successfully")
            } catch (e: Exception) {
                Log.e("GameRepository", "Failed to sync score: ${e.message}")
                // Keep synced = false so we can retry later
            }
        }
    }

    suspend fun getOnlineLeaderboard(): List<HighScoreResponse> {
        return withContext(Dispatchers.IO) {
            try {
                RetrofitClient.api.getTopScores()
            } catch (e: Exception) {
                Log.e("GameRepository", "Failed to fetch leaderboard: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun syncUnsyncedScores() {
        withContext(Dispatchers.IO) {
            try {
                val unsynced = dao.getUnsyncedScores()
                unsynced.forEach { score ->
                    try {
                        val request = HighScoreRequest(
                            score.playerName,
                            score.score,
                            score.difficulty
                        )
                        RetrofitClient.api.submitScore(request)
                        dao.updateScore(score.copy(synced = true))
                    } catch (e: Exception) {
                        Log.e("GameRepository", "Failed to sync score ${score.id}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("GameRepository", "Sync failed: ${e.message}")
            }
        }
    }

    suspend fun clearAllScores() {
        withContext(Dispatchers.IO) {
            dao.clearAll()
        }
    }
}
