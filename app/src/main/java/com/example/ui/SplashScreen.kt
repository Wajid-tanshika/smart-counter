package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000L)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // Main Logo Graphic Row (Robot Head + Smart Counter Text)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.wrapContentSize()
            ) {
                // Robot Icon with + and - buttons
                Box(
                    modifier = Modifier.size(100.dp, 84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Robot Head Dome
                    Box(
                        modifier = Modifier
                            .size(76.dp, 64.dp)
                            .align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Eyes
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.offset(y = (-4).dp)
                        ) {
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(0xFF0EA5E9)))
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color(0xFF0EA5E9)))
                        }
                    }

                    // Green + and Red - buttons held at corners
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Green + Circle
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        // Red - Circle
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("−", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title Text: Smart Counter
                Column {
                    Text(
                        text = "Smart",
                        color = Color(0xFF2563EB),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "Counter",
                        color = Color(0xFF22C55E),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-1).sp,
                        modifier = Modifier.offset(y = (-6).dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Colorful Bottom Sequence: 8 7 6 5 4 3 2 1 0
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.wrapContentWidth()
            ) {
                val numberColors = listOf(
                    Color(0xFFEA580C), // 8: Orange
                    Color(0xFFF59E0B), // 7: Amber
                    Color(0xFFEAB308), // 6: Yellow
                    Color(0xFF84CC16), // 5: Lime
                    Color(0xFF22C55E), // 4: Green
                    Color(0xFF10B981), // 3: Emerald
                    Color(0xFF14B8A6), // 2: Teal
                    Color(0xFF2563EB), // 1: Blue
                    Color(0xFF3B82F6)  // 0: Light Blue
                )
                val numbers = listOf("8", "7", "6", "5", "4", "3", "2", "1", "0")
                numbers.forEachIndexed { index, num ->
                    Text(
                        text = num,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = numberColors.getOrElse(index) { Color.Gray }
                    )
                }
            }
        }
    }
}


