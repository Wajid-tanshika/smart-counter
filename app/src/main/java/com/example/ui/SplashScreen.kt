package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var isReady by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isReady) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isReady) 1f else 0f,
        animationSpec = tween(1000),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        isReady = true
        delay(2500L) // Wait for 2.5 seconds
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
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Robot Icon Mockup
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Eyes
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF0EA5E9)))
                            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF0EA5E9)))
                        }
                    }
                    
                    // Small buttons held by robot
                    Row(
                        modifier = Modifier
                            .width(140.dp)
                            .offset(y = 30.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text Logo
                Column {
                    Text(
                        text = "Smart",
                        color = Color(0xFF3B82F6),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "Counter",
                        color = Color(0xFF22C55E),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-1).sp,
                        modifier = Modifier.offset(y = (-10).dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Bottom Numbers
            Text(
                text = "8 7 6 5 4 3 2 1 0",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFF97316), Color(0xFFEAB308), Color(0xFF3B82F6))
                    )
                ),
                letterSpacing = 4.sp
            )
        }
    }
}
