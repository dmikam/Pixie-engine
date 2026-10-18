package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.dinorun.ui.DinoGameScreen
import com.example.menu.ui.GameSelectorScreen
import com.example.menu.ui.RoadmapScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    MENU,
    DINORUN,
    ROADMAP
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    PixieApp()
                }
            }
        }
    }
}

@Composable
fun PixieApp() {
    var currentScreen by remember { mutableStateOf(AppScreen.MENU) }

    Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
        when (screen) {
            AppScreen.MENU -> {
                GameSelectorScreen(
                    onLaunchDinorun = { currentScreen = AppScreen.DINORUN },
                    onOpenRoadmap = { currentScreen = AppScreen.ROADMAP }
                )
            }
            AppScreen.DINORUN -> {
                DinoGameScreen(
                    onExitToMenu = { currentScreen = AppScreen.MENU }
                )
            }
            AppScreen.ROADMAP -> {
                BackHandler { currentScreen = AppScreen.MENU }
                RoadmapScreen(
                    onBack = { currentScreen = AppScreen.MENU },
                    onLaunchGame = { gameId ->
                        if (gameId == "dinorun") {
                            currentScreen = AppScreen.DINORUN
                        }
                    }
                )
            }
        }
    }
}
