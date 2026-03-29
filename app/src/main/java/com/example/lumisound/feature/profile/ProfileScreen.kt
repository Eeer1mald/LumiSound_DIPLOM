package com.example.lumisound.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import android.net.Uri
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.lumisound.ui.theme.ColorAccentSecondary
import com.example.lumisound.ui.theme.ColorBackground
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart

data class UserStats(
    val tracksListened: Int = 0,
    val tracksRated: Int = 0,
    val playlistsCreated: Int = 0,
    val likedTracks: Int = 0
)

data class MenuItem(
    val icon: ImageVector,
    val label: String,
    val subtitle: String,
    val gradient: Pair<Color, Color>,
    val onClick: () -> Unit
)

@Composable
fun ProfileScreen(
    navController: NavHostController,
    userName: String = "Пользователь",
    stats: UserStats = UserStats(),
    onRatingsClick: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val avatarUri by viewModel.avatarUri.collectAsState()
    
    // uCrop launcher for image cropping
    val cropLauncher = rememberLauncherForActivityResult(
        contract = com.example.lumisound.feature.auth.util.UCropActivityResultContract()
    ) { croppedUri: Uri? ->
        croppedUri?.let { viewModel.onAvatarSelected(it) }
    }
    
    // Image picker for avatar
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            // Запускаем uCrop для обрезки
            cropLauncher.launch(it)
        }
    }
    val menuItems = listOf(
        MenuItem(
            icon = Icons.Default.Star,
            label = "Мои оценки",
            subtitle = "${stats.tracksRated} треков оценено",
            gradient = Pair(GradientStart, GradientEnd),
            onClick = onRatingsClick
        ),
        MenuItem(
            icon = Icons.Default.Favorite,
            label = "Любимые треки",
            subtitle = "${stats.likedTracks} треков",
            gradient = Pair(ColorAccentSecondary, ColorAccentSecondary),
            onClick = {}
        ),
        MenuItem(
            icon = Icons.Default.Schedule,
            label = "История прослушивания",
            subtitle = "Последние 30 дней",
            gradient = Pair(GradientStart, GradientStart),
            onClick = {}
        ),
        MenuItem(
            icon = Icons.Default.Settings,
            label = "Настройки",
            subtitle = "Аккаунт и приложение",
            gradient = Pair(ColorSecondary, ColorSecondary),
            onClick = {}
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding() // Отступ для статус-бара
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1B2E),
                                ColorBackground
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile Avatar with change functionality
                    Box(
                        modifier = Modifier.size(120.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(GradientStart, GradientEnd)
                                    )
                                )
                                .shadow(
                                    elevation = 12.dp,
                                    shape = CircleShape,
                                    spotColor = GradientStart.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = 4.dp,
                                    color = ColorBackground,
                                    shape = CircleShape
                                )
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUri != null) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(avatarUri)
                                        .build(),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    success = {
                                        SubcomposeAsyncImageContent()
                                    }
                                )
                            } else {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Camera icon button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(40.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(GradientStart, GradientEnd)
                                    ),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 3.dp,
                                    color = ColorBackground,
                                    shape = CircleShape
                                )
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change avatar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = userName,
                        color = ColorOnBackground,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Меломан",
                        color = ColorSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Stats Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox(
                    icon = Icons.Default.MusicNote,
                    value = stats.tracksListened.toString(),
                    label = "треков",
                    iconColor = GradientStart,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    icon = Icons.Default.Star,
                    value = stats.tracksRated.toString(),
                    label = "оценок",
                    iconColor = ColorAccentSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    icon = Icons.Default.TrendingUp,
                    value = stats.playlistsCreated.toString(),
                    label = "плейлистов",
                    iconColor = GradientStart,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    icon = Icons.Default.EmojiEvents,
                    value = "12",
                    label = "наград",
                    iconColor = ColorAccentSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Menu Items - используем Column вместо LazyColumn для избежания конфликтов
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 80.dp) // Отступ для нижней навигации
            ) {
                menuItems.forEach { item ->
                    MenuItemCard(
                        item = item,
                        onClick = item.onClick,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Achievement Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
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
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(GradientStart, GradientEnd)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Достижения",
                                color = ColorOnBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Achievement Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("🎵", "⭐", "🔥", "💎", "🎧", "🌟", "🎸", "🎹").forEachIndexed { index, emoji ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(
                                            brush = if (index < 4) {
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        GradientStart.copy(alpha = 0.2f),
                                                        GradientEnd.copy(alpha = 0.2f)
                                                    )
                                                )
                                            } else {
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFF1A1B2E).copy(alpha = 0.4f),
                                                        Color(0xFF1A1B2E).copy(alpha = 0.4f)
                                                    )
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (index < 4) {
                                                GradientStart.copy(alpha = 0.3f)
                                            } else {
                                                Color(0xFF2A2D3E).copy(alpha = 0.2f)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = 24.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation убран - теперь в SwipeableNavHost
    }
}

@Composable
private fun StatBox(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1B2E).copy(alpha = 0.6f),
                        Color(0xFF16182A).copy(alpha = 0.6f)
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
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GradientStart.copy(alpha = 0.2f),
                            GradientEnd.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            color = ColorOnBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            color = ColorSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun MenuItemCard(
    item: MenuItem,
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
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(item.gradient.first, item.gradient.second)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Text
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.label,
                    color = ColorOnBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.subtitle,
                    color = ColorSecondary,
                    fontSize = 12.sp
                )
            }

            // Arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ColorSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}



