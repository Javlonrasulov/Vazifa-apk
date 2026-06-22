package uz.vazifa.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import uz.vazifa.app.BuildConfig
import java.io.File
import java.io.FileOutputStream

object MediaUrl {
    fun resolve(filePath: String, serverUrl: String? = null): String {
        serverUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { url ->
            if (url.startsWith("http://") || url.startsWith("https://")) return url
            val base = BuildConfig.API_BASE_URL
                .removeSuffix("/api/v1/")
                .removeSuffix("/api/v1")
                .removeSuffix("/")
            if (url.startsWith("/")) return "$base$url"
            return "$base/$url"
        }
        return fromFilePath(filePath)
    }

    fun fromFilePath(filePath: String): String {
        val base = BuildConfig.API_BASE_URL
            .removeSuffix("/api/v1/")
            .removeSuffix("/api/v1")
            .removeSuffix("/")
        val normalized = filePath.replace('\\', '/').trim()
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized
        }
        if (normalized.startsWith("/uploads/")) {
            return "$base$normalized"
        }
        if (normalized.startsWith("uploads/")) {
            return "$base/$normalized"
        }
        val fileName = normalized.substringAfterLast('/')
        return "$base/uploads/$fileName"
    }
}

object ImageCompress {
    private const val MAX_EDGE = 1920
    private const val JPEG_QUALITY = 80

    fun compressToFile(context: Context, uri: Uri): File {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Rasm ochilmadi")
        val original = BitmapFactory.decodeStream(input)
        input.close()

        val scale = minOf(
            MAX_EDGE.toFloat() / original.width.coerceAtLeast(1),
            MAX_EDGE.toFloat() / original.height.coerceAtLeast(1),
            1f,
        )
        val w = (original.width * scale).toInt().coerceAtLeast(1)
        val h = (original.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (scale < 1f) Bitmap.createScaledBitmap(original, w, h, true) else original

        val outFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { fos ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
        }
        if (scaled !== original) scaled.recycle()
        original.recycle()
        return outFile
    }

    fun compressToBytes(context: Context, uri: Uri): ByteArray {
        val file = compressToFile(context, uri)
        return file.readBytes().also { file.delete() }
    }
}
