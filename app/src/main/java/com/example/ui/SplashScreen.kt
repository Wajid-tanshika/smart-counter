package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
    LaunchedEffect(Unit) {
        delay(2200L)
        onTimeout()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0369A1), // Sky 700
                        Color(0xFF1D4ED8), // Blue 700
                        Color(0xFF0F172A)  // Slate 900
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .scale(scaleAnim)
        ) {
            // Main Logo Emblem Card
            Surface(
                shape = RoundedCornerShape(36.dp),
                color = Color(0xFF1E3A8A).copy(alpha = 0.85f), // Royal Blue Container
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFF1E40AF), Color(0xFF0EA5E9))
                    )
                ),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Top Hang Ring
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFFE2E8F0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF94A3B8))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Middle Row: [ Goals Clipboard ] - [ Handheld Blue Counter ] - [ Target & Chart ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Goals Checklist Clipboard
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            shadowElevation = 4.dp,
                            modifier = Modifier.width(82.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Yellow Clip
                                Box(
                                    modifier = Modifier
                                        .size(24.dp, 8.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFFF59E0B))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Goals",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                // Water Item
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF0EA5E9)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.height(5.dp).weight(1f).clip(RoundedCornerShape(3.dp)).background(Color(0xFF22C55E)))
                                }
                                Spacer(modifier = Modifier.height(4.dp))

                                // Fitness Item
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF97316)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.height(5.dp).weight(1f).clip(RoundedCornerShape(3.dp)).background(Color(0xFFF59E0B)))
                                }
                                Spacer(modifier = Modifier.height(4.dp))

                                // Book Item
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.height(5.dp).weight(1f).clip(RoundedCornerShape(3.dp)).background(Color(0xFFA855F7)))
                                }
                                Spacer(modifier = Modifier.height(4.dp))

                                // Walk Item
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.height(5.dp).weight(1f).clip(RoundedCornerShape(3.dp)).background(Color(0xFF38BDF8)))
                                }
                            }
                        }

                        // Center: Blue Handheld Tally Counter
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color(0xFF2563EB),
                                border = androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFF1D4ED8)))
                                ),
                                shadowElevation = 8.dp,
                                modifier = Modifier.size(108.dp, 130.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    // Digital Screen Box
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0F172A),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = "0123",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = 2.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }

                                    // Big Metallic Click Button
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFFFFFFFF),
                                                        Color(0xFFCBD5E1),
                                                        Color(0xFF64748B)
                                                    )
                                                )
                                            )
                                            .border(2.dp, Color(0xFF94A3B8), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        // Right: Archery Target & Bar Chart
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.width(82.dp)
                        ) {
                            // Target Bullseye with Red/White Rings
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFBBF24))
                                        )
                                    }
                                }
                            }

                            // 3D Bar Chart with Rising Trend
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Box(modifier = Modifier.width(8.dp).height(16.dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(Color(0xFF38BDF8)))
                                Box(modifier = Modifier.width(8.dp).height(26.dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(Color(0xFFFBBF24)))
                                Box(modifier = Modifier.width(8.dp).height(38.dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(Color(0xFF22C55E)))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4 Feature Icon Badges Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FeatureIconBadge(icon = Icons.Default.WaterDrop, color = Color(0xFF0EA5E9))
                        FeatureIconBadge(icon = Icons.Default.FitnessCenter, color = Color(0xFFF97316))
                        FeatureIconBadge(icon = Icons.Default.MenuBook, color = Color(0xFFA855F7))
                        FeatureIconBadge(icon = Icons.Default.DirectionsWalk, color = Color(0xFF22C55E))
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Main App Name: Smart Counter
                    Text(
                        text = "Smart Counter",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.shadow(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pill Banner: Count Objects • Scan Barcodes • Track Inventory
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFBBF24), // Vibrant Amber Yellow
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "Count • Scan • Inventory",
                            color = Color(0xFF0F172A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureIconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}



