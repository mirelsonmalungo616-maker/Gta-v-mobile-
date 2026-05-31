package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.example.game.GameRenderer
import com.example.game.GameViewModel
import com.example.game.SleekHud
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A))
                            .padding(innerPadding)
                    ) {
                        var wPx by remember { mutableStateOf(800f) }
                        var hPx by remember { mutableStateOf(600f) }

                        // 1. Full Screen Render Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { size ->
                                    wPx = size.width.toFloat()
                                    hPx = size.height.toFloat()
                                }
                        ) {
                            val player = gameViewModel.player
                            
                            // Base Camera offsets centered on target
                            var camX = player.x - wPx / 2f
                            var camY = player.y - hPx / 2f

                            // Add Screen Shake on top for impact (explosions, ramming)
                            if (gameViewModel.screenShakeAmount > 0.1f) {
                                camX += (Math.random().toFloat() * gameViewModel.screenShakeAmount - gameViewModel.screenShakeAmount / 2f)
                                camY += (Math.random().toFloat() * gameViewModel.screenShakeAmount - gameViewModel.screenShakeAmount / 2f)
                            }

                            // Layer 1: Draw roads and sidewalk separators
                            GameRenderer.drawCityBase(
                                drawScope = this,
                                screenWidth = wPx,
                                screenHeight = hPx,
                                cameraX = camX,
                                cameraY = camY
                            )

                            // Layer 2: Draw 3D extruding buildings and houses
                            GameRenderer.drawBuildingsAndHouses3D(
                                drawScope = this,
                                cameraX = camX,
                                cameraY = camY,
                                playerX = player.x,
                                playerY = player.y
                            )

                            // Layer 3: Draw vehicular entities, traffic, pedestrians and active players
                            GameRenderer.drawCarsAndPlayers(
                                drawScope = this,
                                cameraX = camX,
                                cameraY = camY,
                                player = player,
                                carsList = gameViewModel.cars,
                                pedestriansList = gameViewModel.pedestrians
                            )

                            // Layer 4: Floating items, bullet fire, particle sparks, explosions, and checkpoints
                            GameRenderer.drawVfxAndCheckpoints(
                                drawScope = this,
                                cameraX = camX,
                                cameraY = camY,
                                playerX = player.x,
                                playerY = player.y,
                                bullets = gameViewModel.bullets,
                                explosions = gameViewModel.explosions,
                                particles = gameViewModel.particles,
                                activeMission = player.activeMission,
                                customWaypointX = gameViewModel.customWaypointX,
                                customWaypointY = gameViewModel.customWaypointY
                            )
                        }

                        // 2. Playable control overlay and stats scoreboard
                        SleekHud(
                            vm = gameViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        gameViewModel.isPaused = true
    }

    override fun onResume() {
        super.onResume()
        gameViewModel.isPaused = false
    }
}
