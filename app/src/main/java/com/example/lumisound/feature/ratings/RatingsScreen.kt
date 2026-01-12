package com.example.lumisound.feature.ratings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.lumisound.feature.home.TrackPreview
import com.example.lumisound.feature.home.components.BottomNavigationBar
import com.example.lumisound.feature.home.components.TopAppBar
import com.example.lumisound.ui.theme.ColorAccentSecondary
import com.example.lumisound.ui.theme.ColorBackground
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart

data class RatedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val rating: Int,
    val ratedAt: String
)

@Composable
fun RatingsScreen(
    navController: NavHostController,
    ratedTracks: List<RatedTrack> = emptyList(),
    onTrackClick: (String) -> Unit = {}
) {
    var filterRating by remember { mutableStateOf<Int?>(null) } // null = все

    val filteredTracks = if (filterRating == null) {
        ratedTracks
    } else {
        ratedTracks.filter { it.rating == filterRating }
    }

    val averageRating = if (ratedTracks.isNotEmpty()) {
        (ratedTracks.sumOf { it.rating }.toFloat() / ratedTracks.size).toString().take(3)
    } else "0.0"

    val highRatedCount = ratedTracks.count { it.rating >= 8 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding() // Отступ для статус-бара
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(
                userName = "",
                onProfileClick = { navController.navigate("profile") }
            )

            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        GradientStart.copy(alpha = 0.2f),
                                        GradientEnd.copy(alpha = 0.2f)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = GradientStart.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = GradientStart,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "Мои оценки",
                    color = ColorOnBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
                Text(
                    text = "Аналитика вашего музыкального вкуса",
                    color = ColorSecondary,
                    fontSize = 14.sp
                )
            }

            // Stats Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    icon = Icons.Default.Star,
                    label = "Всего",
                    value = ratedTracks.size.toString(),
                    iconColor = ColorAccentSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = "Средняя",
                    value = averageRating,
                    iconColor = GradientStart,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.EmojiEvents,
                    label = "Топ",
                    value = highRatedCount.toString(),
                    iconColor = ColorAccentSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Filter Buttons
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterButton(
                        text = "Все оценки",
                        isSelected = filterRating == null,
                        onClick = { filterRating = null }
                    )
                }
                items(listOf(10, 9, 8, 7, 6, 5)) { rating ->
                    FilterButton(
                        text = "$rating/10",
                        isSelected = filterRating == rating,
                        onClick = { filterRating = rating }
                    )
                }
            }

            // Rated Tracks List
            if (filteredTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1A1B2E),
                                        Color(0xFF16182A)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF2A2D3E).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            GradientStart.copy(alpha = 0.2f),
                                            GradientEnd.copy(alpha = 0.2f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = GradientStart.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(18.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = GradientStart,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Нет оценённых треков",
                            color = ColorOnBackground.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Начните оценивать музыку, чтобы увидеть статистику",
                            color = ColorSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredTracks) { track ->
                        RatedTrackItem(
                            track = track,
                            onClick = { onTrackClick(track.id) }
                        )
                    }
                }
            }
        }

        // Bottom Navigation - с отступом для системной навигации
        BottomNavigationBar(
            currentRoute = "profile",
            onNavigate = { route -> navController.navigate(route) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding() // Отступ для системной навигации
        )
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1B2E),
                        Color(0xFF16182A)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF2A2D3E).copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                color = ColorSecondary,
                fontSize = 12.sp
            )
        }
        Text(
            text = value,
            color = ColorOnBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FilterButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                brush = if (isSelected) {
                    Brush.horizontalGradient(
                        colors = listOf(GradientStart, GradientEnd)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1A1B2E),
                            Color(0xFF1A1B2E)
                        )
                    )
                },
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = Color(0xFF2A2D3E).copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .shadow(
                elevation = if (isSelected) 4.dp else 0.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = GradientStart.copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else ColorSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RatedTrackItem(
    track: RatedTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1B2E).copy(alpha = 0.8f),
                        Color(0xFF16182A).copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF2A2D3E).copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A1B2E),
                                Color(0xFF16182A)
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    color = ColorOnBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = ColorSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.ratedAt,
                    color = GradientStart.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Rating Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = if (track.rating >= 8) {
                            Brush.linearGradient(
                                colors = listOf(GradientStart, GradientEnd)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1A1B2E),
                                    Color(0xFF1A1B2E)
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = if (track.rating >= 8) 0.dp else 1.dp,
                        color = Color(0xFF2A2D3E).copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .shadow(
                        elevation = if (track.rating >= 8) 4.dp else 0.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = GradientStart.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = track.rating.toString(),
                        color = if (track.rating >= 8) Color.White else ColorOnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/10",
                        color = if (track.rating >= 8) Color.White.copy(alpha = 0.7f) else ColorSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}



