package com.example.lumisound.feature.search

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.lumisound.data.model.Track
import com.example.lumisound.feature.search.getPlayerStateHolder
import com.example.lumisound.feature.home.TrackPreview
import com.example.lumisound.feature.home.components.SearchField
import com.example.lumisound.feature.home.components.TopAppBar
import com.example.lumisound.ui.theme.ColorAccentSecondary
import com.example.lumisound.ui.theme.ColorBackground
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart
import kotlinx.coroutines.delay

data class GenreItem(
    val name: String,
    val gradient: Pair<Color, Color>
)

@Composable
fun SearchScreen(
    navController: NavHostController,
    trendingTracks: List<TrackPreview> = emptyList(),
    onTrackClick: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("all") }
    
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val playerStateHolder = getPlayerStateHolder()
    
    // Debounce поиска
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            delay(500) // Задержка 500мс перед поиском
            viewModel.searchTracks(searchQuery)
        } else {
            viewModel.searchTracks("")
        }
    }

    val genres = listOf(
        GenreItem("Поп", Pair(Color(0xFFFF5C6C), Color(0xFFFF8A94))),
        GenreItem("Рок", Pair(Color(0xFF7B6DFF), Color(0xFF9D8FFF))),
        GenreItem("Хип-хоп", Pair(Color(0xFFFF5C6C), Color(0xFF7B6DFF))),
        GenreItem("Электроника", Pair(Color(0xFF00C6FF), Color(0xFF7B6DFF))),
        GenreItem("Джаз", Pair(Color(0xFFFFB347), Color(0xFFFF5C6C))),
        GenreItem("Классика", Pair(GradientStart, GradientEnd))
    )

    val filters = listOf(
        "all" to "Всё",
        "tracks" to "Треки",
        "artists" to "Артисты",
        "albums" to "Альбомы"
    )

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

            // Header with Search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Поиск",
                    color = ColorOnBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                SearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Поиск музыки, исполнителей, жанров",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field")
                )
            }

            // Filter Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { (id, label) ->
                    FilterChip(
                        text = label,
                        isSelected = activeFilter == id,
                        onClick = { activeFilter = id }
                    )
                }
            }

            // Content
            if (searchQuery.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Trending Section
                    if (trendingTracks.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = ColorAccentSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "В тренде",
                                color = ColorOnBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        trendingTracks.forEachIndexed { index, track ->
                            TrendingTrackItem(
                                track = track,
                                rank = index + 1,
                                onClick = { onTrackClick(track.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Genres Section
                    Text(
                        text = "Жанры",
                        color = ColorOnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    genres.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { genre ->
                                GenreCard(
                                    genre = genre,
                                    modifier = Modifier.weight(1f),
                                    onClick = {}
                                )
                            }
                        }
                    }
                }
            } else {
                // Search Results
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = GradientStart
                            )
                        }
                    }
                    error != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
                            ) {
                                Text(
                                    text = "Ошибка поиска",
                                    color = ColorOnBackground,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error ?: "Неизвестная ошибка",
                                    color = ColorSecondary,
                                    fontSize = 14.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    searchResults.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
                            ) {
                                Text(
                                    text = "Ничего не найдено",
                                    color = ColorOnBackground.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Попробуйте изменить запрос",
                                    color = ColorSecondary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(searchResults) { index, track ->
                                SearchResultTrackItem(
                                    track = track,
                                    onClick = { 
                                        // Устанавливаем плейлист из результатов поиска
                                        val allTracks = searchResults.toList()
                                        playerStateHolder.setPlaylist(allTracks, index)
                                        // Навигируем к NowPlaying
                                        navController.navigate("now_playing/${track.id}")
                                    }
                                )
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
private fun FilterChip(
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
private fun TrendingTrackItem(
    track: TrackPreview,
    rank: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
            // Rank
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        brush = if (rank <= 3) {
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
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    color = if (rank <= 3) Color.White else ColorSecondary,
                    fontSize = 14.sp
                )
            }

            // Cover image
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A1B2E),
                                Color(0xFF16182A)
                            )
                        )
                    )
            ) {
                if (track.coverUrl != null && track.coverUrl.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = ColorSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        success = {
                            SubcomposeAsyncImageContent()
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = ColorSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    color = ColorOnBackground,
                    fontSize = 14.sp,
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
            }
        }
    }
}

@Composable
private fun GenreCard(
    genre: GenreItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(96.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(genre.gradient.first, genre.gradient.second)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = genre.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SearchResultTrackItem(
    track: Track,
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
            // Cover image
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A1B2E),
                                Color(0xFF16182A)
                            )
                        )
                    )
            ) {
                if (track.imageUrl != null && track.imageUrl.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = ColorSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        success = {
                            SubcomposeAsyncImageContent()
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = ColorSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.name,
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
                if (track.genre != null) {
                    Text(
                        text = track.genre,
                        color = ColorSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

