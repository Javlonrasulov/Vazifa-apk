package uz.vazifa.app.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object CameraImageUri {
    fun create(context: Context): Uri {
        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }
}
