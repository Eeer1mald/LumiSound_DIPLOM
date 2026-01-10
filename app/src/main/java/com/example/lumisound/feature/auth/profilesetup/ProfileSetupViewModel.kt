package com.example.lumisound.feature.auth.profilesetup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.local.SessionManager
import com.example.lumisound.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

sealed class ProfileSetupSideEffect {
    data object NavigateToHome : ProfileSetupSideEffect()
    data class ShowSnackbar(val message: String) : ProfileSetupSideEffect()
}

data class ProfileSetupUiState(
    val username: String = "",
    val bio: String = "",
    val favoriteGenre: String = "",
    val avatarUri: Uri? = null,
    val usernameError: String? = null,
    val isSubmitting: Boolean = false
)

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        // Загружаем сохраненный username из pending store
        viewModelScope.launch {
            val email = sessionManager.getEmail()
            email?.let {
                val pendingUsername = authRepository.getPendingUsername(it)
                if (pendingUsername != null) {
                    _uiState.value = _uiState.value.copy(username = pendingUsername)
                }
            }
        }
    }
    
    private val _sideEffect = MutableSharedFlow<ProfileSetupSideEffect>()
    val sideEffect = _sideEffect.asSharedFlow()
    
    fun onUsernameChanged(username: String) {
        val error = when {
            username.isBlank() -> null
            username.length < 3 -> "Никнейм должен содержать минимум 3 символа"
            username.length > 20 -> "Никнейм не должен превышать 20 символов"
            !username.matches(Regex("^[a-zA-Zа-яА-ЯёЁ0-9_]+$")) -> "Никнейм может содержать только буквы, цифры и подчеркивания"
            else -> null
        }
        
        _uiState.value = _uiState.value.copy(
            username = username,
            usernameError = error
        )
    }
    
    fun onBioChanged(bio: String) {
        _uiState.value = _uiState.value.copy(bio = bio.take(200))
    }
    
    fun onFavoriteGenreChanged(genre: String) {
        _uiState.value = _uiState.value.copy(favoriteGenre = genre.take(50))
    }
    
    fun onAvatarSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                // Изображение уже обрезано через uCrop, просто сжимаем
                val compressedUri = compressImage(uri)
                _uiState.value = _uiState.value.copy(avatarUri = compressedUri)
            } catch (e: Exception) {
                _sideEffect.emit(ProfileSetupSideEffect.ShowSnackbar("Ошибка обработки изображения: ${e.message}"))
            }
        }
    }
    
    fun submitProfile() {
        viewModelScope.launch {
            if (_uiState.value.usernameError != null || _uiState.value.username.isBlank()) {
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            
            try {
                val accessToken = sessionManager.getAccessToken()
                val email = sessionManager.getEmail()
                
                if (accessToken == null || email == null) {
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                    _sideEffect.emit(ProfileSetupSideEffect.ShowSnackbar("Ошибка: не найден токен доступа"))
                    return@launch
                }
                
                // Upload avatar if present
                var avatarUrl: String? = null
                _uiState.value.avatarUri?.let { uri ->
                    try {
                        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                        val fileBytes = inputStream?.readBytes()
                        inputStream?.close()
                        
                        if (fileBytes != null) {
                            val fileName = "avatar_${email.replace("@", "_").replace(".", "_")}_${System.currentTimeMillis()}.jpg"
                            val userId = email // Используем email как идентификатор для простоты
                            
                            authRepository.uploadAvatar(
                                accessToken = accessToken,
                                userId = userId,
                                fileBytes = fileBytes,
                                fileName = fileName
                            ).onSuccess { uploadedUrl ->
                                avatarUrl = uploadedUrl
                            }.onFailure { exception ->
                                _sideEffect.emit(ProfileSetupSideEffect.ShowSnackbar("Ошибка загрузки аватара: ${exception.message}"))
                            }
                        }
                    } catch (e: Exception) {
                        _sideEffect.emit(ProfileSetupSideEffect.ShowSnackbar("Ошибка обработки аватара: ${e.message}"))
                    }
                }
                
                // Save profile data to database
                // Получаем user ID если он еще не сохранен (для случаев когда пользователь входит впервые после подтверждения email)
                var userId = authRepository.getUserId()
                if (userId == null) {
                    // Если user ID нет, возможно он есть в токене, но не был сохранен
                    // В этом случае используем email для сопоставления (email должен быть уникальным)
                    // Просто передаем null, upsertProfile использует email
                }
                
                authRepository.saveProfile(
                    accessToken = accessToken,
                    username = _uiState.value.username.trim(),
                    email = email,
                    bio = _uiState.value.bio.takeIf { it.isNotBlank() },
                    favoriteGenre = _uiState.value.favoriteGenre.takeIf { it.isNotBlank() },
                    avatarUrl = avatarUrl
                ).onSuccess {
                    // Очищаем pending username после сохранения профиля
                    authRepository.clearPendingUsername(email)
                    _sideEffect.emit(ProfileSetupSideEffect.NavigateToHome)
                }.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                    val errorMessage = exception.message ?: "Неизвестная ошибка"
                    android.util.Log.e("ProfileSetupViewModel", "Ошибка сохранения профиля: $errorMessage", exception)
                    _sideEffect.emit(ProfileSetupSideEffect.ShowSnackbar("Ошибка сохранения: $errorMessage"))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                _sideEffect.emit(ProfileSetupSideEffect.ShowSnackbar("Ошибка сохранения: ${e.message}"))
            }
        }
    }
    
    private suspend fun compressImage(uri: Uri): Uri {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
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
        
        // Save to cache and return URI
        val cacheFile = java.io.File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
        cacheFile.outputStream().use { it.write(byteArray) }
        
        return Uri.fromFile(cacheFile)
    }
}
