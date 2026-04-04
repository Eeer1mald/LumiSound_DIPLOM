package com.example.lumisound.feature.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.lumisound.R
import com.example.lumisound.data.model.Track
import com.example.lumisound.feature.home.components.SearchField
import com.example.lumisound.feature.home.components.TopAppBar
import com.example.lumisound.feature.home.components.TrackCard
import com.example.lumisound.feature.nowplaying.PlayerViewModel
import com.example.lumisound.ui.theme.ColorBackground
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.ColorSurface
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import com.example.lumisound.ui.theme.LumiSoundTheme

@Composable
fun HomeScreen(
    navController: NavHostController,
    userName: String = "Пользователь",
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding() // Отступ для статус-бара
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top App Bar with Logo only
            TopAppBar(
                userName = state.userName.ifEmpty { userName }
            )
            
            // Scrollable Content
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                // Greeting - точные отступы как в Figma
                Column(
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = "Привет, ${state.userName.ifEmpty { userName }}!",
                        color = ColorOnBackground,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Что послушаем сегодня?",
                        color = ColorSecondary,
                        fontSize = 14.sp
                    )
                }

                // Search Bar - точные отступы как в Figma
                SearchField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        // Навигация к экрану поиска при вводе текста
                        if (it.isNotEmpty()) {
                            navController.navigate("search")
                        }
                    },
                    onSearchClick = {
                        if (searchQuery.isNotEmpty()) {
                            navController.navigate("search")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .testTag("home_search")
                )

                // Recommendations Section - точная структура как в Figma
                val isLoading by viewModel.isLoading.collectAsState()
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = GradientStart,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (state.recommendations.isNotEmpty()) {
                    Text(
                        text = "Рекомендации для вас",
                        color = ColorOnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        itemsIndexed(
                            items = state.recommendations,
                            key = { _, item -> item.id },
                            contentType = { _, _ -> "track_card" } // Оптимизация для LazyRow
                        ) { index, item ->
                            TrackCard(
                                track = item,
                                modifier = Modifier,
                                onClick = { 
                                    // Запускаем трек без навигации в плеер
                                    val track = Track(
                                        id = item.id,
                                        name = item.title,
                                        artist = item.artist,
                                        imageUrl = item.coverUrl,
                                        previewUrl = null,
                                        genre = null
                                    )
                                    playerViewModel.playTrack(track)
                                },
                                testTag = "home_card_${index}"
                            )
                        }
                    }
                }

                // Empty State Placeholder - показываем только если нет рекомендаций и не загружается
                if (!isLoading && state.recommendations.isEmpty()) {
                    // Оптимизация: используем remember для градиентов
                    // Заменены градиенты на однотонные цвета
                    val emptyStateColor = remember {
                        ColorSurface // Тёмно-серый вместо градиента
                    }
                    val iconColorSolid = remember {
                        GradientStart // Однотонный акцентный цвет
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 80.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = GradientStart.copy(alpha = 0.1f)
                            )
                            .background(
                                color = emptyStateColor, // Однотонный цвет вместо градиента
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF1F1F1F).copy(alpha = 0.3f), // Тёмно-серый вместо 0xFF2A2D3E
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Иконка - точный размер как в Figma (64dp = w-16 h-16)
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        color = iconColorSolid, // Однотонный цвет вместо градиента
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(18.dp),
                                        spotColor = GradientStart.copy(alpha = 0.3f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Текст - точные размеры как в Figma
                            Text(
                                text = "Скоро здесь появятся ваши рекомендации",
                                color = ColorOnBackground.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Слушайте музыку и оценивайте треки, чтобы мы могли подобрать для вас идеальный плейлист",
                                color = ColorSecondary,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Bottom Navigation убран - теперь в SwipeableNavHost
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun HomeScreenPreview() {
    LumiSoundTheme {
        HomeScreen(navController = rememberNavController(), userName = "Александр")
    }
}


