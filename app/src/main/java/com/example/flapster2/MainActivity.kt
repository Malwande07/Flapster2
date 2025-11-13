package com.example.flapster2

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.min

// Language System - Fixed: removed unused 'code' property
enum class Language(val displayName: String) {
    ENGLISH("English"),
    ZULU("IsiZulu"),
    AFRIKAANS("Afrikaans")
}

object Strings {
    fun get(key: String, language: Language): String {
        return translations[language]?.get(key) ?: translations[Language.ENGLISH]?.get(key) ?: key
    }

    private val translations = mapOf(
        Language.ENGLISH to mapOf(
            "app_name" to "FLAPSTER2",
            "locked" to "Secure Access",
            "authenticate" to "🔓 AUTHENTICATE",
            "auth_instruction" to "Use fingerprint, face or device credentials",
            "play" to "▶ PLAY",
            "leaderboard" to "🏆 LEADERBOARD",
            "settings" to "⚙️ SETTINGS",
            "best" to "🏆 Best:",
            "game_over" to "GAME OVER",
            "score" to "Score:",
            "new_high_score" to "🎉 NEW HIGH SCORE!",
            "play_again" to "PLAY AGAIN",
            "menu" to "MENU",
            "great_score" to "Great Score!",
            "your_name" to "Your Name",
            "submit_score" to "Submit Score",
            "global_leaderboard" to "🏆 GLOBAL LEADERBOARD",
            "back" to "← BACK",
            "refresh" to "🔄 REFRESH",
            "no_scores" to "No scores yet!\nBe the first to play!",
            "difficulty" to "Difficulty",
            "easy" to "Easy",
            "normal" to "Normal",
            "hard" to "Hard",
            "language" to "Language",
            "reset_high_score" to "🗑️ Reset High Score",
            "speed_boost" to "SPEED BOOST!",
            "distance" to "m"
        ),
        Language.ZULU to mapOf(
            "app_name" to "FLAPSTER2",
            "locked" to "Ukufinyelela Okuvikelekile",
            "authenticate" to "🔓 QINISEKISA",
            "auth_instruction" to "Sebenzisa izigxivizo zeminwe, ubuso noma imininingwane yedivayisi",
            "play" to "▶ DLALA",
            "leaderboard" to "🏆 IBHODI LABAHOLI",
            "settings" to "⚙️ IZILUNGISELELO",
            "best" to "🏆 Okungcono:",
            "game_over" to "UMDLALO UPHELE",
            "score" to "Amaphuzu:",
            "new_high_score" to "🎉 AMAPHUZU AMASHA APHEZULU!",
            "play_again" to "DLALA FUTHI",
            "menu" to "IMENYU",
            "great_score" to "Amaphuzu Amahle!",
            "your_name" to "Igama Lakho",
            "submit_score" to "Thumela Amaphuzu",
            "global_leaderboard" to "🏆 IBHODI LABAHOLI LOMHLABA",
            "back" to "← BUYELA",
            "refresh" to "🔄 VUSELELA",
            "no_scores" to "Awukho amaphuzu okwamanje!\nBa owokuqala ukudlala!",
            "difficulty" to "Ubunzima",
            "easy" to "Kulula",
            "normal" to "Okuvamile",
            "hard" to "Kunzima",
            "language" to "Ulimi",
            "reset_high_score" to "🗑️ Setha Kabusha Amaphuzu Aphezulu",
            "speed_boost" to "UKWENYUKA KWEJUBANE!",
            "distance" to "m"
        ),
        Language.AFRIKAANS to mapOf(
            "app_name" to "FLAPSTER2",
            "locked" to "Veilige Toegang",
            "authenticate" to "🔓 VERIFIEER",
            "auth_instruction" to "Gebruik vingerafdruk, gesig of toestelgeloofsbriewe",
            "play" to "▶ SPEEL",
            "leaderboard" to "🏆 LEIERSBORD",
            "settings" to "⚙️ INSTELLINGS",
            "best" to "🏆 Beste:",
            "game_over" to "SPELETJIE VERBY",
            "score" to "Telling:",
            "new_high_score" to "🎉 NUWE HOËTELLING!",
            "play_again" to "SPEEL WEER",
            "menu" to "KIESLYS",
            "great_score" to "Groot Telling!",
            "your_name" to "Jou Naam",
            "submit_score" to "Dien Telling In",
            "global_leaderboard" to "🏆 WÊRELDWYE LEIERSBORD",
            "back" to "← TERUG",
            "refresh" to "🔄 VERFRIS",
            "no_scores" to "Nog geen tellings nie!\nWees die eerste om te speel!",
            "difficulty" to "Moeilikheidsgraad",
            "easy" to "Maklik",
            "normal" to "Normaal",
            "hard" to "Moeilik",
            "language" to "Taal",
            "reset_high_score" to "🗑️ Herstel Hoëtelling",
            "speed_boost" to "SPOEDVERHOGING!",
            "distance" to "m"
        )
    )
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var biometricCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

            LaunchedEffect(biometricCallback) {
                biometricCallback?.let { callback ->
                    showBiometricPrompt(callback)
                    biometricCallback = null
                }
            }

            Flapster2Game(
                onRequestBiometric = { onSuccess ->
                    biometricCallback = onSuccess
                },
                isBiometricAvailable = checkBiometricAvailability()
            )
        }
    }

    private fun checkBiometricAvailability(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Authenticate to access Flapster2")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }
}

enum class GameState {
    BIOMETRIC_LOCK, MENU, PLAYING, GAME_OVER, SETTINGS, LEADERBOARD, NAME_INPUT
}

data class Pipe(
    var x: Float,
    val height: Float,
    var scored: Boolean = false
)

data class Coin(
    var x: Float,
    var y: Float,
    var collected: Boolean = false
)

data class GameSettings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val showFPS: Boolean = false,
    val language: Language = Language.ENGLISH
)

// Difficulty enum - Fixed: removed unused 'label' property
enum class Difficulty(val speedMultiplier: Float) {
    EASY(0.7f),
    NORMAL(1.0f),
    HARD(1.5f)
}

@Composable
fun Flapster2Game(
    onRequestBiometric: (onSuccess: () -> Unit) -> Unit,
    isBiometricAvailable: Boolean
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { GameRepository(database.highScoreDao()) }
    val scope = rememberCoroutineScope()

    var gameState by remember { mutableStateOf(if (isBiometricAvailable) GameState.BIOMETRIC_LOCK else GameState.MENU) }
    var birdY by remember { mutableFloatStateOf(250f) }
    var birdVelocity by remember { mutableFloatStateOf(0f) }
    var pipes by remember { mutableStateOf(listOf<Pipe>()) }
    var coins by remember { mutableStateOf(listOf<Coin>()) }
    var score by remember { mutableIntStateOf(0) }
    var distance by remember { mutableIntStateOf(0) }
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
        coins = emptyList()
        score = 0
        distance = 0
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

                // Update distance
                distance += (0.1f * difficultyMultiplier).toInt()

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

                // Update coins
                coins = coins.map { coin ->
                    coin.copy(x = coin.x - 4f * difficultyMultiplier)
                }.filter { it.x > -30f && !it.collected }

                // Spawn coins randomly
                if (coins.size < 3 && (0..100).random() < 2) {
                    coins = coins + Coin(
                        x = gameWidth,
                        y = (150..700).random().toFloat()
                    )
                }

                val birdX = 150f
                val coinSize = 30f

                // Check coin collection
                coins.forEach { coin ->
                    if (!coin.collected &&
                        birdX + birdSize > coin.x && birdX < coin.x + coinSize &&
                        birdY + birdSize > coin.y && birdY < coin.y + coinSize
                    ) {
                        coin.collected = true
                        score += 5 // Bonus points for collecting coins
                    }
                }

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
                coins = coins,
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
                Text(
                    text = "$distance${Strings.get("distance", settings.language)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF).copy(alpha = 0.8f)
                )
                if (difficultyMultiplier > 1.5f) {
                    Text(
                        text = "${Strings.get("speed_boost", settings.language)} ${String.format(Locale.US, "%.1f", difficultyMultiplier)}x",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF44F)
                    )
                }
            }
        }

        // Biometric Lock Screen
        if (gameState == GameState.BIOMETRIC_LOCK) {
            BiometricLockScreen(
                language = settings.language,
                onAuthenticateClick = {
                    onRequestBiometric {
                        gameState = GameState.MENU
                    }
                }
            )
        }

        // Menu screen
        if (gameState == GameState.MENU) {
            MenuScreen(
                highScore = highScore,
                language = settings.language,
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
                language = settings.language,
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
                language = settings.language,
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
                language = settings.language,
                onPlayAgain = { startGame() },
                onMenuClick = { gameState = GameState.MENU }
            )
        }
    }
}

// Biometric Lock Screen
@Composable
fun BiometricLockScreen(
    language: Language,
    onAuthenticateClick: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
        Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            Text(
                "🔒",
                fontSize = 100.sp
            )
            Text(
                Strings.get("app_name", language),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                Strings.get("locked", language),
                fontSize = 24.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onAuthenticateClick,
                Modifier.width(280.dp).height(65.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))
            ) {
                Text(
                    Strings.get("authenticate", language),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                Strings.get("auth_instruction", language),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
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
    coins: List<Coin>,
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

        // Coins
        coins.forEach { coin ->
            if (!coin.collected) {
                val coinSize = 30f * scale
                // Outer gold circle
                drawCircle(
                    Color(0xFFFFD700),
                    coinSize / 2,
                    Offset(coin.x * scale + coinSize / 2, coin.y * scale + coinSize / 2)
                )
                // Inner circle for depth
                drawCircle(
                    Color(0xFFFFA500),
                    coinSize / 3,
                    Offset(coin.x * scale + coinSize / 2, coin.y * scale + coinSize / 2)
                )
            }
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
    language: Language,
    onPlayClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLeaderboardClick: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(Strings.get("app_name", language), fontSize = 72.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("${Strings.get("best", language)} $highScore", fontSize = 32.sp, color = Color(0xFFFFF44F))
            Spacer(Modifier.height(40.dp))
            Button(onClick = onPlayClick, Modifier.width(250.dp).height(60.dp), colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))) {
                Text(Strings.get("play", language), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onLeaderboardClick, Modifier.width(250.dp).height(60.dp), colors = ButtonDefaults.buttonColors(Color(0xFFFF9800))) {
                Text(Strings.get("leaderboard", language), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onSettingsClick, Modifier.width(250.dp).height(60.dp), colors = ButtonDefaults.buttonColors(Color(0xFF2196F3))) {
                Text(Strings.get("settings", language), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Name Input Screen
@Composable
fun NameInputScreen(
    score: Int,
    playerName: String,
    language: Language,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), Alignment.Center) {
        Column(
            Modifier.padding(30.dp).background(Color.White).padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(Strings.get("great_score", language), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Blue)
            Text("${Strings.get("score", language)} $score", fontSize = 24.sp, color = Color.Black)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = playerName,
                onValueChange = onNameChange,
                label = { Text(Strings.get("your_name", language)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onSubmit, Modifier.width(200.dp)) {
                Text(Strings.get("submit_score", language), fontSize = 18.sp)
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
            Text(Strings.get("settings", settings.language), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(40.dp))

            // Language Selection
            Text(Strings.get("language", settings.language), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(20.dp))

            Language.entries.forEach { lang ->
                Button(
                    onClick = { onSettingsChange(settings.copy(language = lang)) },
                    Modifier.width(280.dp).height(55.dp),
                    colors = ButtonDefaults.buttonColors(if (settings.language == lang) Color(0xFF4CAF50) else Color.Gray)
                ) {
                    Text(lang.displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(30.dp))

            Text(Strings.get("difficulty", settings.language), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(20.dp))

            Difficulty.entries.forEach { diff ->
                val diffLabel = when (diff) {
                    Difficulty.EASY -> Strings.get("easy", settings.language)
                    Difficulty.NORMAL -> Strings.get("normal", settings.language)
                    Difficulty.HARD -> Strings.get("hard", settings.language)
                }
                Button(
                    onClick = { onSettingsChange(settings.copy(difficulty = diff)) },
                    Modifier.width(280.dp).height(55.dp),
                    colors = ButtonDefaults.buttonColors(if (settings.difficulty == diff) Color(0xFF4CAF50) else Color.Gray)
                ) {
                    Text(diffLabel, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onResetHighScore,
                Modifier.width(280.dp).height(55.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFFFF5722))
            ) {
                Text(Strings.get("reset_high_score", settings.language), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onBackClick,
                Modifier.width(200.dp).height(55.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF4CAF50))
            ) {
                Text(Strings.get("back", settings.language), fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
    language: Language,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f))) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Spacer(Modifier.height(40.dp))
            Text(
                Strings.get("global_leaderboard", language),
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
                    Text(Strings.get("back", language))
                }
                Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(Color(0xFF2196F3))) {
                    Text(Strings.get("refresh", language))
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
                        Strings.get("no_scores", language),
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
    language: Language,
    onPlayAgain: () -> Unit,
    onMenuClick: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(Strings.get("game_over", language), fontSize = 60.sp, fontWeight = FontWeight.Bold, color = Color.Red)
            Column(
                Modifier.background(Color.White).padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${Strings.get("score", language)} $score", fontSize = 32.sp, color = Color.Blue)
                Text("${Strings.get("best", language)} $highScore", fontSize = 24.sp, color = Color(0xFFFFD700))
                if (isNewHighScore) {
                    Text(
                        Strings.get("new_high_score", language),
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
                Text(Strings.get("play_again", language), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onMenuClick,
                Modifier.width(250.dp).height(60.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF2196F3))
            ) {
                Text(Strings.get("menu", language), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}