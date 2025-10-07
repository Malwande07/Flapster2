package com.example.flapster2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Flapster2Game()
        }
    }
}

enum class GameState {
    MENU, PLAYING, GAME_OVER, SETTINGS, LEADERBOARD, NAME_INPUT
}

data class Pipe(
    var x: Float,
    val height: Float,
    var scored: Boolean = false
)

data class GameSettings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val showFPS: Boolean = false
)

enum class Difficulty(val label: String, val speedMultiplier: Float) {
    EASY("Easy", 0.7f),
    NORMAL("Normal", 1.0f),
    HARD("Hard", 1.5f)
}

@Composable
fun Flapster2Game() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { GameRepository(database.highScoreDao()) }
    val scope = rememberCoroutineScope()

    var gameState by remember { mutableStateOf(GameState.MENU) }
    var birdY by remember { mutableFloatStateOf(250f) }
    var birdVelocity by remember { mutableFloatStateOf(0f) }
    var pipes by remember { mutableStateOf(listOf<Pipe>()) }
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(0) }
    var colorIndex by remember { mutableIntStateOf(0) }
    var difficultyMultiplier by remember { mutableFloatStateOf(1f) }
    var settings by remember { mutableStateOf(GameSettings()) }
    var playerName by remember { mutableStateOf("") }
    var onlineLeaderboard by remember { mutableStateOf<List<HighScoreResponse>>(emptyList()) }
    var isLoadingLeaderboard by remember { mutableStateOf(false) }

    // Load high score on start
    LaunchedEffect(Unit) {
        highScore = repository.getHighestScore()
        repository.syncUnsyncedScores()
    }

    val colors = listOf(
        Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFF45B7D1),
        Color(0xFFFFA07A), Color(0xFF98D8C8), Color(0xFFF7DC6F)
    )

    val gravity = 0.6f
    val jumpStrength = -10f
    val pipeWidth = 80f
    val pipeGap = 220f
    val birdSize = 40f
    val gameWidth = 1080f
    val gameHeight = 1920f

    fun startGame() {
        gameState = GameState.PLAYING
        birdY = 250f
        birdVelocity = 0f
        pipes = listOf(Pipe(gameWidth, 300f))
        score = 0
        difficultyMultiplier = settings.difficulty.speedMultiplier
        colorIndex = 0
    }

    fun jump() {
        if (gameState == GameState.PLAYING) {
            birdVelocity = jumpStrength
        }
    }

    fun endGame() {
        if (score > highScore) {
            highScore = score
        }
        if (score > 0) {
            gameState = GameState.NAME_INPUT
        } else {
            gameState = GameState.GAME_OVER
        }
    }

    // Game loop
    LaunchedEffect(gameState) {
        if (gameState == GameState.PLAYING) {
            while (isActive && gameState == GameState.PLAYING) {
                delay(16)
                difficultyMultiplier = settings.difficulty.speedMultiplier * (1f + score * 0.05f)
                birdVelocity += gravity
                birdY += birdVelocity

                if (birdY > gameHeight - birdSize - 150f || birdY < 0f) {
                    endGame()
                    continue
                }

                pipes = pipes.map { pipe ->
                    pipe.copy(x = pipe.x - 4f * difficultyMultiplier)
                }.filter { it.x > -pipeWidth }

                pipes = if (pipes.isEmpty() || pipes.last().x < gameWidth - 350f) {
                    pipes + Pipe(gameWidth, (100..600).random().toFloat())
                } else {
                    pipes
                }

                val birdX = 150f
                pipes.forEach { pipe ->
                    if (!pipe.scored && pipe.x + pipeWidth < birdX) {
                        pipe.scored = true
                        score++
                        colorIndex = (colorIndex + 1) % colors.size
                    }

                    if (birdX + birdSize > pipe.x && birdX < pipe.x + pipeWidth &&
                        (birdY < pipe.height || birdY + birdSize > pipe.height + pipeGap)
                    ) {
                        endGame()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF87CEEB), Color(0xFFADD8E6))))
            .pointerInput(Unit) {
                detectTapGestures {
                    when (gameState) {
                        GameState.PLAYING -> jump()
                        else -> {}
                    }
                }
            }
    ) {
        // Game Canvas
        if (gameState == GameState.PLAYING) {
            GameCanvas(
                birdY = birdY,
                birdVelocity = birdVelocity,
                pipes = pipes,
                colors = colors,
                colorIndex = colorIndex,
                gameWidth = gameWidth,
                birdSize = birdSize,
                pipeWidth = pipeWidth,
                pipeGap = pipeGap
            )
        }

        // Score display
        if (gameState == GameState.PLAYING) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = score.toString(), fontSize = 80.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (difficultyMultiplier > 1.5f) {
                    Text(
                        text = "SPEED BOOST! ${String.format(Locale.US, "%.1f", difficultyMultiplier)}x",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF44F)
                    )
                }
            }
        }

        // Menu screen
        if (gameState == GameState.MENU) {
            MenuScreen(
                highScore = highScore,
                onPlayClick = { startGame() },
                onSettingsClick = { gameState = GameState.SETTINGS },
                onLeaderboardClick = {
                    gameState = GameState.LEADERBOARD
                    isLoadingLeaderboard = true
                    scope.launch {
                        onlineLeaderboard = repository.getOnlineLeaderboard()
                        isLoadingLeaderboard = false
                    }
                }
            )
        }

        // Name Input Screen
        if (gameState == GameState.NAME_INPUT) {
            NameInputScreen(
                score = score,
                playerName = playerName,
                onNameChange = { playerName = it },
                onSubmit = {
                    scope.launch {
                        repository.saveScore(
                            playerName.ifBlank { "Anonymous" },
                            score,
                            settings.difficulty.name
                        )
                    }
                    gameState = GameState.GAME_OVER
                }
            )
        }

        // Settings screen
        if (gameState == GameState.SETTINGS) {
            SettingsScreen(
                settings = settings,
                onSettingsChange = { settings = it },
                onResetHighScore = {
                    scope.launch {
                        repository.clearAllScores()
                        highScore = 0
                    }
                },
                onBackClick = { gameState = GameState.MENU }
            )
        }

        // Leaderboard screen
        if (gameState == GameState.LEADERBOARD) {
            LeaderboardScreen(
                onlineScores = onlineLeaderboard,
                isLoading = isLoadingLeaderboard,
                onRefresh = {
                    isLoadingLeaderboard = true
                    scope.launch {
                        onlineLeaderboard = repository.getOnlineLeaderboard()
                        isLoadingLeaderboard = false
                    }
                },
                onBackClick = { gameState = GameState.MENU }
            )
        }

        // Game over screen
        if (gameState == GameState.GAME_OVER) {
            GameOverScreen(
                score = score,
                highScore = highScore,
                isNewHighScore = score == highScore && score > 0,
                onPlayAgain = { startGame() },
                onMenuClick = { gameState = GameState.MENU }
            )
        }
    }
}

// Game Canvas Component
@Composable
fun GameCanvas(
    birdY: Float,
    birdVelocity: Float,
    pipes: List<Pipe>,
    colors: List<Color>,
    colorIndex: Int,
    gameWidth: Float,
    birdSize: Float,
    pipeWidth: Float,
    pipeGap: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scale = size.width / gameWidth

        // Clouds
        drawCircle(Color.White.copy(alpha = 0.7f), 60f * scale, Offset(200f * scale, 150f * scale))
        drawCircle(Color.White.copy(alpha = 0.6f), 70f * scale, Offset(800f * scale, 250f * scale))

        // Pipes
        pipes.forEach { pipe ->
            drawRect(Color(0xFF228B22), Offset(pipe.x * scale, 0f), Size(pipeWidth * scale, pipe.height * scale))
            drawRect(Color(0xFF1B6B1B), Offset(pipe.x * scale, (pipe.height - 30f) * scale), Size(pipeWidth * scale, 30f * scale))
            val bottomTop = (pipe.height + pipeGap) * scale
            drawRect(Color(0xFF228B22), Offset(pipe.x * scale, bottomTop), Size(pipeWidth * scale, size.height - bottomTop - 150f * scale))
            drawRect(Color(0xFF1B6B1B), Offset(pipe.x * scale, bottomTop), Size(pipeWidth * scale, 30f * scale))
        }

        // Bird
        rotate(min(birdVelocity * 3f, 45f), Offset(150f * scale + birdSize * scale / 2, birdY * scale + birdSize * scale / 2)) {
            drawCircle(colors[colorIndex], birdSize * scale / 2, Offset(150f * scale + birdSize * scale / 2, birdY * scale + birdSize * scale / 2))
            drawCircle(Color.White, 6f * scale, Offset(165f * scale, (birdY + 10f) * scale))
            drawCircle(Color.Black, 3f * scale, Offset(167f * scale, (birdY + 11f) * scale))
        }

        // Ground
        drawRect(Color(0xFFD2691E), Offset(0f, size.height - 150f * scale), Size(size.width, 150f * scale))
    }
}

// Menu Screen
@Composable
fun MenuScreen(
    highScore: Int,
    onPlayClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLeaderboardClick: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("FLAPSTER2", fontSize = 72.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("🏆 Best: $highScore", fontSize = 32.sp, color = Color(0xFFFFF44F))
            Spacer(Modifier.height(40.dp))
            Button(onClick = onPlayClick, Modifier.width(250.dp).height(60.dp), colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))) {
                Text("▶ PLAY", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onLeaderboardClick, Modifier.width(250.dp).height(60.dp), colors = ButtonDefaults.buttonColors(Color(0xFFFF9800))) {
                Text("🏆 LEADERBOARD", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onSettingsClick, Modifier.width(250.dp).height(60.dp), colors = ButtonDefaults.buttonColors(Color(0xFF2196F3))) {
                Text("⚙️ SETTINGS", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Name Input Screen
@Composable
fun NameInputScreen(
    score: Int,
    playerName: String,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), Alignment.Center) {
        Column(
            Modifier.padding(30.dp).background(Color.White).padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Great Score!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
            Text("Score: $score", fontSize = 24.sp, color = Color.Black)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = playerName,
                onValueChange = onNameChange,
                label = { Text("Your Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onSubmit, Modifier.width(200.dp)) {
                Text("Submit Score", fontSize = 18.sp)
            }
        }
    }
}

// Settings Screen
@Composable
fun SettingsScreen(
    settings: GameSettings,
    onSettingsChange: (GameSettings) -> Unit,
    onResetHighScore: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f))) {
        Column(
            Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Text("⚙️ SETTINGS", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(40.dp))

            Text("Difficulty", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(20.dp))

            Difficulty.entries.forEach { diff ->
                Button(
                    onClick = { onSettingsChange(settings.copy(difficulty = diff)) },
                    Modifier.width(280.dp).height(55.dp),
                    colors = ButtonDefaults.buttonColors(if (settings.difficulty == diff) Color(0xFF4CAF50) else Color.Gray)
                ) {
                    Text(diff.label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onResetHighScore,
                Modifier.width(280.dp).height(55.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFFFF5722))
            ) {
                Text("🗑️ Reset High Score", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onBackClick,
                Modifier.width(200.dp).height(55.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))
            ) {
                Text("← BACK", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// Leaderboard Screen
@Composable
fun LeaderboardScreen(
    onlineScores: List<HighScoreResponse>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f))) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Spacer(Modifier.height(40.dp))
            Text(
                "🏆 GLOBAL LEADERBOARD",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onBackClick, colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))) {
                    Text("← BACK")
                }
                Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(Color(0xFF2196F3))) {
                    Text("🔄 REFRESH")
                }
            }

            Spacer(Modifier.height(20.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (onlineScores.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        "No scores yet!\nBe the first to play!",
                        fontSize = 24.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(onlineScores) { index, scoreData ->
                        LeaderboardItem(
                            rank = index + 1,
                            playerName = scoreData.playerName,
                            score = scoreData.score,
                            difficulty = scoreData.difficulty
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(rank: Int, playerName: String, score: Int, difficulty: String) {
    val backgroundColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color.White.copy(alpha = 0.1f)
    }

    val textColor = if (rank <= 3) Color.Black else Color.White

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$rank",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.width(50.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = playerName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = difficulty,
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
            Text(
                text = score.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

// Game Over Screen
@Composable
fun GameOverScreen(
    score: Int,
    highScore: Int,
    isNewHighScore: Boolean,
    onPlayAgain: () -> Unit,
    onMenuClick: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("GAME OVER", fontSize = 60.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            Column(
                Modifier.background(Color.White).padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Score: $score", fontSize = 32.sp, color = Color.Blue)
                Text("🏆 Best: $highScore", fontSize = 24.sp, color = Color(0xFFFFD700))
                if (isNewHighScore) {
                    Text(
                        "🎉 NEW HIGH SCORE!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Green
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onPlayAgain,
                Modifier.width(250.dp).height(60.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))
            ) {
                Text("PLAY AGAIN", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onMenuClick,
                Modifier.width(250.dp).height(60.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF2196F3))
            ) {
                Text("MENU", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}