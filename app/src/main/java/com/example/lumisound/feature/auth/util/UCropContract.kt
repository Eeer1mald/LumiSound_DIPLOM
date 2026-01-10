package com.example.lumisound.feature.auth.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.yalantis.ucrop.UCrop
import java.io.File

class UCropActivityResultContract : ActivityResultContract<Uri, Uri?>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        val destinationUri = Uri.fromFile(File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        
        return UCrop.of(input, destinationUri)
            .withAspectRatio(1f, 1f) // Квадрат
            .withMaxResultSize(1024, 1024)
            .getIntent(context)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode == Activity.RESULT_OK && intent != null) {
            return UCrop.getOutput(intent)
        }
        return null
    }
}
