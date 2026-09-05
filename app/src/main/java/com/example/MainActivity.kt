package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdManager
import com.example.data.AppThemeMode
import com.example.ui.CounterSubScreen
import com.example.ui.MainNavigationTab
import com.example.ui.SmartCounterViewModel
import com.example.ui.SplashScreen
import com.example.ui.components.AppBottomNavigation
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.ManualCounterScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceCounterModal
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SmartCounterViewModel by viewModels()
    private lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        adManager = AdManager.getInstance(this)

        setContent {
            val themeMode by viewModel.settings.themeMode.collectAsStateWithLifecycle()
            val isOnboardingDone by viewModel.settings.isOnboardingCompleted.collectAsStateWithLifecycle()

            val isDark = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            var isSplashVisible by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = isDark) {
                if (isSplashVisible) {
                    SplashScreen(onTimeout = {
                        isSplashVisible = false
                        adManager.showAppOpenAdIfAvailable(this@MainActivity)
                    })
                } else if (!isOnboardingDone) {
                    OnboardingScreen(
                        onFinish = {
                            viewModel.settings.completeOnboarding()
                        }
                    )
                } else {
                    MainAppContent(
                        viewModel = viewModel,
                        adManager = adManager,
                        activity = this@MainActivity
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: SmartCounterViewModel,
    adManager: AdManager,
    activity: ComponentActivity
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val activeSubScreen by viewModel.activeSubScreen.collectAsStateWithLifecycle()

    // Back button handling
    BackHandler(enabled = activeSubScreen != CounterSubScreen.NONE || currentTab != MainNavigationTab.HOME) {
        if (activeSubScreen != CounterSubScreen.NONE) {
            viewModel.closeSubScreen()
            adManager.showInterstitialIfAllowed(activity)
        } else if (currentTab != MainNavigationTab.HOME) {
            viewModel.selectTab(MainNavigationTab.HOME)
        }
    }

    if (activeSubScreen != CounterSubScreen.NONE) {
        when (activeSubScreen) {
            CounterSubScreen.MANUAL_COUNTER -> {
                ManualCounterScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        viewModel.closeSubScreen()
                        adManager.showInterstitialIfAllowed(activity)
                    },
                    onOpenVoiceCounter = {
                        viewModel.openSubScreen(CounterSubScreen.VOICE_COUNTER)
                    },
                    onOpenObjectCounter = {
                        viewModel.selectTab(MainNavigationTab.SCAN)
                        viewModel.closeSubScreen()
                    }
                )
            }
            CounterSubScreen.VOICE_COUNTER -> {
                VoiceCounterModal(
                    viewModel = viewModel,
                    onClose = {
                        viewModel.openSubScreen(CounterSubScreen.MANUAL_COUNTER)
                    }
                )
            }
            CounterSubScreen.OBJECT_COUNTER -> {
                viewModel.selectTab(MainNavigationTab.SCAN)
                viewModel.closeSubScreen()
            }
            CounterSubScreen.PRIVACY_POLICY -> {
                PrivacyPolicyScreen(
                    onNavigateBack = {
                        viewModel.closeSubScreen()
                    }
                )
            }
            else -> {
                viewModel.closeSubScreen()
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                AppBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        viewModel.selectTab(tab)
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    MainNavigationTab.HOME -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToTab = { tab ->
                                viewModel.selectTab(tab)
                            },
                            onOpenSubScreen = { subScreen ->
                                viewModel.openSubScreen(subScreen)
                            }
                        )
                    }
                    MainNavigationTab.SCAN -> {
                        ScannerScreen(
                            viewModel = viewModel,
                            onNavigateToInventory = {
                                viewModel.selectTab(MainNavigationTab.INVENTORY)
                            }
                        )
                    }
                    MainNavigationTab.HISTORY -> {
                        HistoryScreen(viewModel = viewModel)
                    }
                    MainNavigationTab.INVENTORY -> {
                        InventoryScreen(
                            viewModel = viewModel,
                            onOpenScanner = {
                                viewModel.selectTab(MainNavigationTab.SCAN)
                            }
                        )
                    }
                    MainNavigationTab.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onOpenPrivacyPolicy = {
                                viewModel.openSubScreen(CounterSubScreen.PRIVACY_POLICY)
                            },
                            onReplayOnboarding = {
                                viewModel.settings.resetOnboarding()
                            }
                        )
                    }
                }
            }
        }
    }
}
