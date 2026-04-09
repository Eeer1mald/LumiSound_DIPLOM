package com.example.lumisound.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import android.net.Uri
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.lumisound.feature.home.TrackPreview
import com.example.lumisound.ui.theme.ColorAccentSecondary
import com.example.lumisound.ui.theme.ColorBackground
import com.example.lumisound.ui.theme.ColorOnBackground
import com.example.lumisound.ui.theme.ColorSecondary
import com.example.lumisound.ui.theme.ColorSurface
import com.example.lumisound.ui.theme.GradientEnd
import com.example.lumisound.ui.theme.GradientStart


@Composable
fun ProfileScreen(
    navController: NavHostController,
    onRatingsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onArtistClick: (artistId: String, artistName: String, artistImageUrl: String?) -> Unit = { _, _, _ -> },
    onTrackClick: (trackId: String, title: String, artist: String, coverUrl: String?, previewUrl: String?) -> Unit = { _, _, _, _, _ -> },
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val avatarUri by viewModel.avatarUri.collectAsState()
    
    val username = uiState.username.ifEmpty { "Пользователь" }
    val bio = uiState.bio
    
    // Запоминаем списки стабильно
    val favoriteTracks = uiState.favoriteTracks
    val favoriteArtists = uiState.favoriteArtists
    
    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var showEditBioDialog by remember { mutableStateOf(false) }
    var editedUsername by remember { mutableStateOf("") }
    var editedBio by remember { mutableStateOf("") }
    
    // uCrop launcher for image cropping
    val cropLauncher = rememberLauncherForActivityResult(
        contract = com.example.lumisound.feature.auth.util.UCropActivityResultContract()
    ) { croppedUri: Uri? ->
        if (croppedUri != null) {
            android.util.Log.d("ProfileScreen", "Получен результат от UCrop: $croppedUri")
            viewModel.onAvatarSelected(croppedUri)
        } else {
            android.util.Log.w("ProfileScreen", "UCrop вернул null - возможно пользователь отменил обрезку")
        }
    }
    
    // Image picker for avatar
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                android.util.Log.d("ProfileScreen", "Выбран URI: $selectedUri, схема: ${selectedUri.scheme}")
                // Запускаем uCrop для обрезки
                cropLauncher.launch(selectedUri)
            } catch (e: Exception) {
                android.util.Log.e("ProfileScreen", "Ошибка запуска обрезки: ${e.message}", e)
            }
        }
    }
    
    // Данные загружаются только при инициализации ViewModel (в init блоке)
    // Не обновляем при каждом переходе на экран
    
    val avatarImageRequest = remember(avatarUri, uiState.avatarUrl) {
        // Приоритет: локальный avatarUri (только что обрезанный), затем avatarUrl из БД
        val imageUri = avatarUri ?: (uiState.avatarUrl?.let { Uri.parse(it) })
        imageUri?.let { uri ->
            ImageRequest.Builder(context)
                .data(uri)
                .crossfade(false) // Отключаем crossfade для лучшей производительности
                .placeholder(android.R.drawable.ic_menu_gallery) // Placeholder пока загружается
                .error(android.R.drawable.ic_menu_report_image) // Иконка при ошибке
                .build()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding()
    ) {
        // Используем remember для scrollState чтобы избежать пересоздания при recomposition
        // Оптимизация для 120Hz: graphicsLayer для кеширования скролла
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 140.dp) // место для мини-плеера + bottom nav
        ) {
            // Header with dark background - используем remember для оптимизации
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorBackground)
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
                        // Заменён градиент на однотонный цвет
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(color = GradientStart) // Однотонный акцентный цвет
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 4.dp,
                                    color = ColorBackground,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarImageRequest != null) {
                                SubcomposeAsyncImage(
                                    model = avatarImageRequest,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        // Показываем первую букву во время загрузки
                                        Text(
                                            text = username.take(1).uppercase(),
                                            color = Color.White,
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    error = {
                                        // Если изображение не загрузилось, показываем первую букву
                                        Text(
                                            text = username.take(1).uppercase(),
                                            color = Color.White,
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    success = {
                                        SubcomposeAsyncImageContent()
                                    }
                                )
                            } else {
                                Text(
                                    text = username.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Camera icon button - заменён градиент на однотонный цвет
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(40.dp)
                                .background(
                                    color = GradientStart, // Однотонный акцентный цвет
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

                    // Username - кликабельный для редактирования
                    Text(
                        text = username,
                        color = ColorOnBackground,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            editedUsername = uiState.username
                            showEditUsernameDialog = true
                        }
                    )

                    // Bio - серый квадратик со скругленными краями
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = Color(0xFF1F1F1F).copy(alpha = 0.3f), // Тёмно-серый вместо 0xFF2A2D3E
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                editedBio = bio ?: ""
                                showEditBioDialog = true
                            }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = bio ?: "О себе",
                            color = if (bio != null) ColorOnBackground else ColorSecondary,
                            fontSize = 14.sp,
                            style = if (bio == null) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Settings icon in top right
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                ) {
                    // Заменён градиент на однотонный цвет
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = ColorSurface.copy(alpha = 0.8f), // Тёмно-серый вместо градиента
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF1F1F1F).copy(alpha = 0.3f), // Тёмно-серый вместо 0xFF2A2D3E
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = ColorOnBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Stats Grid — комментарии и рецензии
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox(
                    icon = Icons.Default.MusicNote,
                    value = uiState.commentsCount.toString(),
                    label = "комментариев",
                    iconColor = GradientStart,
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    icon = Icons.Default.Star,
                    value = uiState.reviewsCount.toString(),
                    label = "рецензий",
                    iconColor = ColorAccentSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Favorite Tracks Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Любимые треки сейчас",
                        color = ColorOnBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (favoriteTracks.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        items(
                            items = favoriteTracks,
                            key = { it.id },
                            contentType = { "track_card" }
                        ) { track ->
                            FavoriteTrackCard(
                                track = track,
                                onClick = {
                                    onTrackClick(
                                        track.trackId,
                                        track.title,
                                        track.artist,
                                        track.coverUrl,
                                        track.previewUrl
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Начните слушать музыку, чтобы увидеть ваши любимые треки",
                        color = ColorSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            // Favorite Artists Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Любимые исполнители сейчас",
                        color = ColorOnBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (favoriteArtists.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        items(
                            items = favoriteArtists,
                            key = { it.id },
                            contentType = { "artist_card" }
                        ) { artist ->
                            FavoriteArtistCard(
                                artist = artist,
                                onClick = { onArtistClick(artist.artistId, artist.name, artist.imageUrl) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Начните слушать музыку, чтобы увидеть ваших любимых артистов",
                        color = ColorSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            // Menu Items - убрана строка "Мои оценки"
        }
    }
    
    // Edit Username Dialog
    if (showEditUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showEditUsernameDialog = false },
            title = { Text("Редактировать имя") },
            text = {
                OutlinedTextField(
                    value = editedUsername,
                    onValueChange = { editedUsername = it },
                    label = { Text("Имя пользователя") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editedUsername.isNotBlank()) {
                            viewModel.updateUsername(editedUsername)
                            showEditUsernameDialog = false
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUsernameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Edit Bio Dialog
    if (showEditBioDialog) {
        AlertDialog(
            onDismissRequest = { showEditBioDialog = false },
            title = { Text("Редактировать \"О себе\"") },
            text = {
                OutlinedTextField(
                    value = editedBio,
                    onValueChange = { editedBio = it },
                    label = { Text("О себе") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBio(editedBio.takeIf { it.isNotBlank() })
                        showEditBioDialog = false
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBioDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun StatBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    // Заменены градиенты на однотонные цвета
    val statBoxColor = remember {
        ColorSurface.copy(alpha = 0.6f) // Тёмно-серый вместо градиента
    }
    val iconColorSolid = remember {
        GradientStart.copy(alpha = 0.2f) // Однотонный акцентный цвет
    }
    Column(
        modifier = modifier
            .background(
                color = statBoxColor,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF1F1F1F).copy(alpha = 0.3f), // Тёмно-серый вместо 0xFF2A2D3E
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = iconColorSolid,
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
private fun FavoriteTrackCard(
    track: FavoriteTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Заменён градиент на однотонный цвет
    val trackCardColor = remember {
        ColorSurface // Тёмно-серый вместо градиента
    }
    Column(
        modifier = modifier
            .width(104.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(104.dp)
                .height(104.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(color = trackCardColor) // Однотонный цвет вместо градиента
                .border(
                    width = 1.dp,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.3f), // Тёмно-серый вместо 0xFF2A2D3E
                    shape = RoundedCornerShape(13.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (track.coverUrl != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.coverUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = track.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(13.dp)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = GradientStart.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = GradientStart.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    success = {
                        SubcomposeAsyncImageContent()
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = GradientStart.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Text(
            text = track.title,
            color = ColorOnBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = ColorSecondary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FavoriteArtistCard(
    artist: FavoriteArtist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Заменён градиент на однотонный цвет
    val artistCardColor = remember {
        ColorSurface // Тёмно-серый вместо градиента
    }
    Column(
        modifier = modifier
            .width(89.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(89.dp)
                .clip(CircleShape)
                .background(color = artistCardColor) // Однотонный цвет вместо градиента
                .border(
                    width = 1.dp,
                    color = Color(0xFF1F1F1F).copy(alpha = 0.3f), // Тёмно-серый вместо 0xFF2A2D3E
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (artist.imageUrl != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artist.imageUrl)
                        .crossfade(false)
                        .build(),
                    contentDescription = artist.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = GradientStart.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = GradientStart.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    success = {
                        SubcomposeAsyncImageContent()
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = GradientStart.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Text(
            text = artist.name,
            color = ColorOnBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MenuItemCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    gradient: Pair<Color, Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Заменены градиенты на однотонные цвета
    val menuCardColor = remember {
        ColorSurface.copy(alpha = 0.8f) // Тёмно-серый вместо градиента
    }
    val iconColorSolid = remember(gradient) {
        // Используем первый цвет градиента или средний акцентный цвет
        GradientStart.copy(alpha = 0.5f) // Однотонный акцентный цвет
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = menuCardColor, // Однотонный цвет вместо градиента
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF1F1F1F).copy(alpha = 0.3f), // Тёмно-серый вместо 0xFF2A2D3E
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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = iconColorSolid, // Однотонный цвет вместо градиента
                        shape = RoundedCornerShape(12.dp)
                    )
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    color = ColorOnBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = ColorSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
