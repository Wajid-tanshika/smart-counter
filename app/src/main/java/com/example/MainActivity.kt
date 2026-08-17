package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ads.AdManager
import com.example.ui.CounterScreen
import com.example.ui.CounterViewModel
import com.example.ui.GoalsScreen
import com.example.ui.GoalsViewModel
import com.example.ui.theme.MyApplicationTheme

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

enum class AppScreen {
  COUNTER,
  GOALS
}

class MainActivity : ComponentActivity() {
  private val counterViewModel: CounterViewModel by viewModels()
  private val goalsViewModel: GoalsViewModel by viewModels()
  private lateinit var adManager: AdManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Google Mobile Ads with requested ad units
    adManager = AdManager.getInstance(this)

    setContent {
      val theme by counterViewModel.theme.collectAsStateWithLifecycle()
      val isDark = theme == com.example.ui.AppTheme.DARK ||
              theme == com.example.ui.AppTheme.NEON_BLUE ||
              theme == com.example.ui.AppTheme.RED_GAMING ||
              theme == com.example.ui.AppTheme.NEON_GREEN ||
              theme == com.example.ui.AppTheme.GOLD ||
              theme == com.example.ui.AppTheme.PURPLE
      
      var isSplashVisible by remember { mutableStateOf(true) }
      var currentScreen by remember { mutableStateOf(AppScreen.COUNTER) }
      var selectedGoalPageIndex by remember { mutableStateOf(0) }
      val allGoals by goalsViewModel.allGoals.collectAsStateWithLifecycle()

      MyApplicationTheme(darkTheme = isDark) {
        if (isSplashVisible) {
          com.example.ui.SplashScreen(onTimeout = {
            isSplashVisible = false
            // Show App Open Ad gracefully on startup if available
            adManager.showAppOpenAdIfAvailable(this@MainActivity)
          })
        } else {
          when (currentScreen) {
            AppScreen.COUNTER -> {
              CounterScreen(
                counterViewModel = counterViewModel,
                goalsViewModel = goalsViewModel,
                initialGoalPage = selectedGoalPageIndex,
                onNavigateToGoals = {
                  goalsViewModel.resetSelectedState()
                  currentScreen = AppScreen.GOALS
                  // Low-frequency non-intrusive interstitial check
                  adManager.showInterstitialIfAllowed(this@MainActivity)
                }
              )
            }
            AppScreen.GOALS -> {
              BackHandler {
                currentScreen = AppScreen.COUNTER
                // Low-frequency non-intrusive interstitial check on screen change
                adManager.showInterstitialIfAllowed(this@MainActivity)
              }
              GoalsScreen(
                goalsViewModel = goalsViewModel,
                counterViewModel = counterViewModel,
                onNavigateBack = {
                  currentScreen = AppScreen.COUNTER
                  adManager.showInterstitialIfAllowed(this@MainActivity)
                },
                onSelectGoalForCounter = { selectedGoal ->
                  val goalIdx = allGoals.indexOfFirst { it.id == selectedGoal.id }
                  selectedGoalPageIndex = if (goalIdx >= 0) goalIdx + 1 else 0
                  currentScreen = AppScreen.COUNTER
                  adManager.showInterstitialIfAllowed(this@MainActivity)
                }
              )
            }
          }
        }
      }
    }
  }
}




