package com.example.lumisound.feature.profile

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri = _avatarUri.asStateFlow()
    
    fun onAvatarSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                // Изображение уже обрезано через uCrop, просто сжимаем
                val compressedUri = compressImage(uri)
                _avatarUri.value = compressedUri
                
                // Upload to Supabase Storage
                val accessToken = sessionManager.getAccessToken()
                val email = sessionManager.getEmail()
                
                if (accessToken != null && email != null) {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(compressedUri)
                    val fileBytes = inputStream?.readBytes()
                    inputStream?.close()
                    
                    if (fileBytes != null) {
                        val fileName = "avatar_${email.replace("@", "_").replace(".", "_")}_${System.currentTimeMillis()}.jpg"
                        val userId = email
                        
                        authRepository.uploadAvatar(
                            accessToken = accessToken,
                            userId = userId,
                            fileBytes = fileBytes,
                            fileName = fileName
                        ).onSuccess { uploadedUrl ->
                            // Update profile with new avatar URL
                            authRepository.saveProfile(
                                accessToken = accessToken,
                                username = "", // Will be fetched from existing profile
                                email = email,
                                bio = null,
                                favoriteGenre = null,
                                avatarUrl = uploadedUrl
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
