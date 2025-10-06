package com.example.flapster2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    MENU, PLAYING, GAME_OVER
}

data class Pipe(
    var x: Float,
    val height: Float,
    var scored: Boolean = false
)

@Composable
fun Flapster2Game() {
    var gameState by remember { mutableStateOf(GameState.MENU) }
    var birdY by remember { mutableFloatStateOf(250f) }
    var birdVelocity by remember { mutableFloatStateOf(0f) }
    var pipes by remember { mutableStateOf(listOf<Pipe>()) }
    var score by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(0) }
    var colorIndex by remember { mutableIntStateOf(0) }
    var difficultyMultiplier by remember { mutableFloatStateOf(1f) }

    val colors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFF45B7D1),
        Color(0xFFFFA07A),
        Color(0xFF98D8C8),
        Color(0xFFF7DC6F)
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
        difficultyMultiplier = 1f
        colorIndex = 0
    }

    fun jump() {
        if (gameState == GameState.PLAYING) {
            birdVelocity = jumpStrength
        }
    }

    // Game loop
    LaunchedEffect(gameState) {
        if (gameState == GameState.PLAYING) {
            while (isActive && gameState == GameState.PLAYING) {
                delay(16) // ~60 FPS

                // Update difficulty
                difficultyMultiplier = 1f + score * 0.05f

                // Update bird
                birdVelocity += gravity
                birdY += birdVelocity

                // Check ground/ceiling collision
                if (birdY > gameHeight - birdSize - 150f || birdY < 0f) {
                    if (score > highScore) highScore = score
                    gameState = GameState.GAME_OVER
                    continue
                }

                // Update pipes
                pipes = pipes.map { pipe ->
                    pipe.copy(x = pipe.x - 4f * difficultyMultiplier)
                }.filter { it.x > -pipeWidth }

                // Add new pipe
                if (pipes.isEmpty() || pipes.last().x < gameWidth - 350f) {
                    pipes = pipes + Pipe(
                        gameWidth,
                        (100..600).random().toFloat()
                    )
                }

                // Check collision and scoring
                val birdX = 150f
                pipes.forEach { pipe ->
                    // Score
                    if (!pipe.scored && pipe.x + pipeWidth < birdX) {
                        pipe.scored = true
                        score++
                        colorIndex = (colorIndex + 1) % colors.size
                    }

                    // Collision
                    if (birdX + birdSize > pipe.x &&
                        birdX < pipe.x + pipeWidth &&
                        (birdY < pipe.height || birdY + birdSize > pipe.height + pipeGap)
                    ) {
                        if (score > highScore) highScore = score
                        gameState = GameState.GAME_OVER
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF87CEEB),
                        Color(0xFFADD8E6)
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures {
                    when (gameState) {
                        GameState.MENU -> startGame()
                        GameState.PLAYING -> jump()
                        GameState.GAME_OVER -> startGame()
                    }
                }
            }
    ) {
        // Game Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = size.width / gameWidth

            // Background clouds
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = 60f * scale,
                center = Offset(200f * scale, 150f * scale)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = 70f * scale,
                center = Offset(800f * scale, 250f * scale)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = 50f * scale,
                center = Offset(500f * scale, 400f * scale)
            )

            // Draw pipes
            pipes.forEach { pipe ->
                // Top pipe
                drawRect(
                    color = Color(0xFF228B22),
                    topLeft = Offset(pipe.x * scale, 0f),
                    size = Size(pipeWidth * scale, pipe.height * scale)
                )
                drawRect(
                    color = Color(0xFF1B6B1B),
                    topLeft = Offset(pipe.x * scale, (pipe.height - 30f) * scale),
                    size = Size(pipeWidth * scale, 30f * scale)
                )

                // Bottom pipe
                val bottomPipeTop = (pipe.height + pipeGap) * scale
                drawRect(
                    color = Color(0xFF228B22),
                    topLeft = Offset(pipe.x * scale, bottomPipeTop),
                    size = Size(pipeWidth * scale, size.height - bottomPipeTop - 150f * scale)
                )
                drawRect(
                    color = Color(0xFF1B6B1B),
                    topLeft = Offset(pipe.x * scale, bottomPipeTop),
                    size = Size(pipeWidth * scale, 30f * scale)
                )
            }

            // Draw bird
            rotate(
                degrees = min(birdVelocity * 3f, 45f),
                pivot = Offset(150f * scale + birdSize * scale / 2, birdY * scale + birdSize * scale / 2)
            ) {
                drawCircle(
                    color = colors[colorIndex],
                    radius = birdSize * scale / 2,
                    center = Offset(150f * scale + birdSize * scale / 2, birdY * scale + birdSize * scale / 2)
                )
                // Eye
                drawCircle(
                    color = Color.White,
                    radius = 6f * scale,
                    center = Offset(165f * scale, (birdY + 10f) * scale)
                )
                drawCircle(
                    color = Color.Black,
                    radius = 3f * scale,
                    center = Offset(167f * scale, (birdY + 11f) * scale)
                )
            }

            // Ground
            drawRect(
                color = Color(0xFFD2691E),
                topLeft = Offset(0f, size.height - 150f * scale),
                size = Size(size.width, 150f * scale)
            )
        }

        // Score display
        if (gameState == GameState.PLAYING) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = score.toString(),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (difficultyMultiplier > 1.5f) {
                    Text(
                        text = "SPEED BOOST! ${String.format(Locale.US, "%.1f", difficultyMultiplier)}x",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFF44F)
                    )
                }
            }
        }

        // Menu screen
        if (gameState == GameState.MENU) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "FLAPSTER2",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "🏆 Best: $highScore",
                        fontSize = 32.sp,
                        color = Color(0xFFFFF44F)
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text = "▶ TAP TO START",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎨 Color changes with each pipe", color = Color.White, fontSize = 16.sp)
                        Text("⚡ Speed increases as you score", color = Color.White, fontSize = 16.sp)
                        Text("🏆 Beat your high score!", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }

        // Game over screen
        if (gameState == GameState.GAME_OVER) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "GAME OVER",
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    Column(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Score: $score",
                            fontSize = 32.sp,
                            color = Color.Blue
                        )
                        Text(
                            text = "🏆 Best: $highScore",
                            fontSize = 24.sp,
                            color = Color(0xFFFFD700)
                        )
                        if (score == highScore && score > 0) {
                            Text(
                                text = "🎉 NEW HIGH SCORE!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Green
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "TAP TO PLAY AGAIN",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}