package com.example.lumisound.feature.auth.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import java.io.File

class UCropActivityResultContract : ActivityResultContract<Uri, Uri?>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        try {
            android.util.Log.d("UCrop", "Создание Intent для обрезки. Входной URI: $input, схема: ${input.scheme}")
            
            // Создаем файл для обрезанного изображения
            val outputFile = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
            outputFile.parentFile?.mkdirs()
            
            // Используем FileProvider для создания URI (необходимо на Android 10+)
            val destinationUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    outputFile
                )
            } catch (e: Exception) {
                android.util.Log.e("UCrop", "Ошибка создания destination URI через FileProvider: ${e.message}")
                // Fallback на file:// если FileProvider не работает (не должно происходить)
                Uri.fromFile(outputFile)
            }
            
            android.util.Log.d("UCrop", "Destination URI: $destinationUri")
            
            // Для content:// URI оставляем как есть, UCrop должен уметь с ним работать
            // Для file:// конвертируем через FileProvider
            val sourceUri = if (input.scheme == "file") {
                try {
                    val sourceFile = File(input.path ?: "")
                    if (sourceFile.exists()) {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            sourceFile
                        ).also {
                            android.util.Log.d("UCrop", "Конвертирован file:// URI в content://: $it")
                        }
                    } else {
                        android.util.Log.w("UCrop", "Файл не существует: ${sourceFile.path}")
                        input
                    }
                } catch (e: Exception) {
                    android.util.Log.w("UCrop", "Не удалось конвертировать source URI: ${e.message}")
                    input
                }
            } else {
                input
            }
            
            android.util.Log.d("UCrop", "Source URI: $sourceUri")
            
            val ucropIntent = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f) // Квадрат
                .withMaxResultSize(1024, 1024)
                .getIntent(context)
            
            // Добавляем флаги для доступа к URI
            ucropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ucropIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            
            android.util.Log.d("UCrop", "Intent создан успешно, компонент: ${ucropIntent.component}")
            return ucropIntent
        } catch (e: Exception) {
            android.util.Log.e("UCrop", "Ошибка создания Intent для обрезки: ${e.message}", e)
            throw e
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        android.util.Log.d("UCrop", "parseResult вызван: resultCode=$resultCode, intent=$intent")
        
        return when {
            resultCode == Activity.RESULT_OK && intent != null -> {
                try {
                    val outputUri = UCrop.getOutput(intent)
                    android.util.Log.d("UCrop", "Успешная обрезка, результат URI: $outputUri")
                    outputUri
                } catch (e: Exception) {
                    android.util.Log.e("UCrop", "Ошибка получения результата обрезки: ${e.message}", e)
                    null
                }
            }
            resultCode == UCrop.RESULT_ERROR && intent != null -> {
                try {
                    val cropError = UCrop.getError(intent)
                    android.util.Log.e("UCrop", "Ошибка обрезки: ${cropError?.message}")
                } catch (e: Exception) {
                    android.util.Log.e("UCrop", "Ошибка парсинга ошибки обрезки: ${e.message}", e)
                }
                null
            }
            resultCode == Activity.RESULT_CANCELED -> {
                android.util.Log.d("UCrop", "Пользователь отменил обрезку")
                null
            }
            else -> {
                android.util.Log.w("UCrop", "Неизвестный resultCode: $resultCode")
                null
            }
        }
    }
}
