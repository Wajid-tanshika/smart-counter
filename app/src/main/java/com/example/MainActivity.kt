package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.CounterScreen
import com.example.ui.CounterViewModel
import com.example.ui.theme.MyApplicationTheme

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
  private val viewModel: CounterViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val theme by viewModel.theme.collectAsStateWithLifecycle()
      val isDark = theme == com.example.ui.AppTheme.DARK || theme == com.example.ui.AppTheme.NEON_BLUE || theme == com.example.ui.AppTheme.RED_GAMING || theme == com.example.ui.AppTheme.NEON_GREEN || theme == com.example.ui.AppTheme.GOLD || theme == com.example.ui.AppTheme.PURPLE
      
      var isSplashVisible by remember { mutableStateOf(true) }

      MyApplicationTheme(darkTheme = isDark) {
        if (isSplashVisible) {
          com.example.ui.SplashScreen(onTimeout = { isSplashVisible = false })
        } else {
          CounterScreen(
            viewModel = viewModel
          )
        }
      }
    }
  }
}


