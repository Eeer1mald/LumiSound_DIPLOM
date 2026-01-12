package com.example.lumisound.feature.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.File
import javax.inject.Inject

data class ProfileUiState(
    val username: String = "",
    val bio: String? = null,
    val avatarUrl: String? = null,
    val favoriteGenre: String? = null,
    val favoriteTracks: List<FavoriteTrack> = emptyList(),
    val favoriteArtists: List<FavoriteArtist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class FavoriteTrack(
    val id: String,
    val trackId: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val previewUrl: String? = null,
    val addedAt: String
)

data class FavoriteArtist(
    val id: String,
    val artistId: String,
    val name: String,
    val imageUrl: String? = null,
    val playCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()
    
    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri = _avatarUri.asStateFlow()
    
    init {
        loadProfile()
        loadFavoriteTracks()
        loadFavoriteArtists()
    }
    
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val accessToken = sessionManager.getAccessToken()
            
            if (accessToken != null) {
                authRepository.getProfile(accessToken)
                    .onSuccess { profile ->
                        if (profile != null) {
                            _uiState.value = _uiState.value.copy(
                                username = profile.username,
                                bio = profile.bio,
                                avatarUrl = profile.avatarUrl,
                                favoriteGenre = profile.favoriteGenre,
                                isLoading = false
                            )
                            // Загружаем аватар если есть URL из Supabase
                            profile.avatarUrl?.let { url ->
                                try {
                                    android.util.Log.d("ProfileViewModel", "Загружаем аватар из URL: $url")
                                    _avatarUri.value = Uri.parse(url)
                                } catch (e: Exception) {
                                    android.util.Log.e("ProfileViewModel", "Ошибка парсинга URL аватара: ${e.message}")
                                    _avatarUri.value = null
                                }
                            } ?: run {
                                // Если нет URL, очищаем локальный URI
                                _avatarUri.value = null
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Профиль не найден"
                            )
                        }
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Ошибка загрузки профиля"
                        )
                    }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Не авторизован"
                )
            }
        }
    }
    
    fun loadFavoriteTracks() {
        viewModelScope.launch {
            val accessToken = sessionManager.getAccessToken()
            if (accessToken != null) {
                // Загружаем топ-10 треков по количеству прослушиваний
                authRepository.getFavoriteTracks(accessToken, limit = 10, orderByPlayCount = true)
                    .onSuccess { tracks ->
                        val favoriteTracks = tracks.map { track ->
                            FavoriteTrack(
                                id = track.id,
                                trackId = track.trackId,
                                title = track.trackTitle,
                                artist = track.trackArtist,
                                coverUrl = track.trackCoverUrl,
                                previewUrl = track.trackPreviewUrl,
                                addedAt = track.addedAt
                            )
                        }
                        _uiState.value = _uiState.value.copy(favoriteTracks = favoriteTracks)
                    }
            }
        }
    }
    
    fun loadFavoriteArtists() {
        viewModelScope.launch {
            val accessToken = sessionManager.getAccessToken()
            if (accessToken != null) {
                // Загружаем топ-10 артистов по количеству прослушиваний
                authRepository.getFavoriteArtists(accessToken, limit = 10)
                    .onSuccess { artists ->
                        val favoriteArtists = artists.map { artist ->
                            FavoriteArtist(
                                id = artist.id,
                                artistId = artist.artistId,
                                name = artist.artistName,
                                imageUrl = artist.artistImageUrl,
                                playCount = artist.playCount
                            )
                        }
                        _uiState.value = _uiState.value.copy(favoriteArtists = favoriteArtists)
                    }
            }
        }
    }
    
    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            val accessToken = sessionManager.getAccessToken()
            val email = sessionManager.getEmail()
            
            if (accessToken != null && email != null) {
                val currentState = _uiState.value
                authRepository.saveProfile(
                    accessToken = accessToken,
                    username = newUsername,
                    email = email,
                    bio = currentState.bio,
                    favoriteGenre = currentState.favoriteGenre,
                    avatarUrl = currentState.avatarUrl
                ).onSuccess {
                    _uiState.value = currentState.copy(username = newUsername)
                }
            }
        }
    }
    
    fun updateBio(newBio: String?) {
        viewModelScope.launch {
            val accessToken = sessionManager.getAccessToken()
            val email = sessionManager.getEmail()
            
            if (accessToken != null && email != null) {
                val currentState = _uiState.value
                authRepository.saveProfile(
                    accessToken = accessToken,
                    username = currentState.username,
                    email = email,
                    bio = newBio,
                    favoriteGenre = currentState.favoriteGenre,
                    avatarUrl = currentState.avatarUrl
                ).onSuccess {
                    _uiState.value = currentState.copy(bio = newBio)
                }
            }
        }
    }
    
    fun onAvatarSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ProfileViewModel", "onAvatarSelected вызван с URI: $uri")
                
                // Изображение уже обрезано через uCrop, просто сжимаем
                val compressedUri = compressImage(uri)
                android.util.Log.d("ProfileViewModel", "Изображение сжато, URI: $compressedUri")
                
                // Сразу показываем обрезанное изображение локально
                _avatarUri.value = compressedUri
                
                // Upload to Supabase Storage
                val accessToken = sessionManager.getAccessToken()
                val email = sessionManager.getEmail()
                
                if (accessToken != null && email != null) {
                    android.util.Log.d("ProfileViewModel", "Начинаем загрузку аватара в Supabase")
                    
                    val inputStream: InputStream? = context.contentResolver.openInputStream(compressedUri)
                    val fileBytes = inputStream?.readBytes()
                    inputStream?.close()
                    
                    if (fileBytes != null) {
                        android.util.Log.d("ProfileViewModel", "Размер файла: ${fileBytes.size} байт")
                        
                        // Получаем userId из сессии или из токена
                        val userId = sessionManager.getUserId()
                        if (userId == null) {
                            android.util.Log.w("ProfileViewModel", "UserId не найден в сессии, пытаемся получить из токена")
                            // Можно попробовать получить userId из токена через authRepository
                            // Но для простоты используем email как fallback
                        }
                        
                        // Используем userId если есть, иначе email (но лучше всегда использовать UUID)
                        val fileIdentifier = userId ?: email.replace("@", "_").replace(".", "_")
                        val fileName = "avatar_${fileIdentifier}_${System.currentTimeMillis()}.jpg"
                        android.util.Log.d("ProfileViewModel", "Имя файла для загрузки: $fileName")
                        
                        authRepository.uploadAvatar(
                            accessToken = accessToken,
                            userId = userId ?: fileIdentifier, // Используем userId или fileIdentifier
                            fileBytes = fileBytes,
                            fileName = fileName
                        ).onSuccess { uploadedUrl ->
                            android.util.Log.d("ProfileViewModel", "Аватар загружен в Supabase Storage, URL: $uploadedUrl")
                            
                            // Update profile with new avatar URL
                            val currentState = _uiState.value
                            authRepository.saveProfile(
                                accessToken = accessToken,
                                username = currentState.username,
                                email = email,
                                bio = currentState.bio,
                                favoriteGenre = currentState.favoriteGenre,
                                avatarUrl = uploadedUrl
                            ).onSuccess {
                                android.util.Log.d("ProfileViewModel", "Профиль обновлен с новым URL аватара в БД")
                                _uiState.value = currentState.copy(avatarUrl = uploadedUrl)
                                // Обновляем локальный URI на публичный URL из Supabase
                                // Это позволит приложению всегда загружать актуальное изображение
                                _avatarUri.value = Uri.parse(uploadedUrl)
                            }.onFailure { exception ->
                                android.util.Log.e("ProfileViewModel", "Ошибка сохранения профиля: ${exception.message}", exception)
                                _uiState.value = _uiState.value.copy(error = "Не удалось сохранить URL аватара: ${exception.message}")
                            }
                        }.onFailure { exception ->
                            android.util.Log.e("ProfileViewModel", "Ошибка загрузки аватара в Supabase Storage: ${exception.message}", exception)
                            _uiState.value = _uiState.value.copy(error = "Не удалось загрузить аватар: ${exception.message}")
                        }
                    } else {
                        android.util.Log.e("ProfileViewModel", "Не удалось прочитать файл")
                    }
                } else {
                    android.util.Log.e("ProfileViewModel", "Нет accessToken или email")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Ошибка в onAvatarSelected: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun compressImage(uri: Uri): Uri {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        if (originalBitmap == null) {
            android.util.Log.e("ProfileViewModel", "Не удалось декодировать bitmap из URI: $uri")
            return uri // Возвращаем оригинальный URI если не удалось обработать
        }
        
        // Resize to reasonable size (512x512 max)
        val maxSize = 512
        val resizedBitmap = if (originalBitmap.width > maxSize || originalBitmap.height > maxSize) {
            val scale = maxSize.toFloat() / maxOf(originalBitmap.width, originalBitmap.height)
            val matrix = Matrix().apply { postScale(scale, scale) }
            Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true).also {
                originalBitmap.recycle()
            }
        } else {
            originalBitmap
        }
        
        // Compress to max 1MB
        var quality = 90
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        var byteArray = outputStream.toByteArray()
        
        while (byteArray.size > 1024 * 1024 && quality > 30) {
            quality -= 10
            outputStream.reset()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            byteArray = outputStream.toByteArray()
        }
        
        resizedBitmap.recycle()
        
        // Save to cache and return URI using FileProvider
        val cacheFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
        cacheFile.parentFile?.mkdirs()
        cacheFile.outputStream().use { it.write(byteArray) }
        
        // Используем FileProvider для создания URI (необходимо на Android 10+)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )
    }
}
