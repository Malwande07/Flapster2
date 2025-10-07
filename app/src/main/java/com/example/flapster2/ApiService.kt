package com.example.flapster2

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.google.gson.annotations.SerializedName

// Data model for API
data class HighScoreResponse(
    val id: Int,
    @SerializedName("player_name")
    val playerName: String,
    val score: Int,
    val difficulty: String,
    @SerializedName("created_at")
    val createdAt: String
)

data class HighScoreRequest(
    @SerializedName("player_name")
    val playerName: String,
    val score: Int,
    val difficulty: String
)

// Retrofit API Interface
interface SupabaseApi {
    @GET("high_scores?select=*&order=score.desc&limit=10")
    suspend fun getTopScores(): List<HighScoreResponse>

    @POST("high_scores")
    suspend fun submitScore(@Body score: HighScoreRequest): HighScoreResponse

    @Suppress("unused") // Reserved for future use
    @GET("high_scores?select=*&order=created_at.desc&limit=1")
    suspend fun getMyLastScore(): List<HighScoreResponse>
}

// Retrofit Client
object RetrofitClient {
    // REPLACE THESE WITH YOUR SUPABASE VALUES!
    private const val SUPABASE_URL = "https://wbomvewkfphveavpkjrf.supabase.co/rest/v1/"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Indib212ZXdrZnBodmVhdnBranJmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk3NTE4MDIsImV4cCI6MjA3NTMyNzgwMn0.mBongK-c9KpfnKx1pLmqPM8q7BS70fnvMFHgR0oNgiw"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build()
            chain.proceed(request)
        }
        .build()

    val api: SupabaseApi by lazy {
        Retrofit.Builder()
            .baseUrl(SUPABASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseApi::class.java)
    }
}