package uz.vazifa.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import uz.vazifa.app.BuildConfig
import java.io.File
import java.io.FileOutputStream

object MediaUrl {
    fun attachmentApiUrl(attachmentId: String): String {
        val base = BuildConfig.API_BASE_URL.removeSuffix("/")
        return "$base/tasks/attachments/$attachmentId/file"
    }

    fun resolve(filePath: String, serverUrl: String? = null): String {
        extractUploadPath(serverUrl)?.let { return "${apiOrigin()}$it" }
        return fromFilePath(filePath)
    }

    fun fromFilePath(filePath: String): String {
        extractUploadPath(filePath)?.let { return "${apiOrigin()}$it" }
        val fileName = filePath.replace('\\', '/').substringAfterLast('/')
        return "${apiOrigin()}/uploads/$fileName"
    }

    private fun apiOrigin(): String = BuildConfig.API_BASE_URL
        .removeSuffix("/api/v1/")
        .removeSuffix("/api/v1")
        .removeSuffix("/")

    private fun extractUploadPath(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val normalized = raw.replace('\\', '/').trim()
        val idx = normalized.indexOf("/uploads/")
        if (idx >= 0) return normalized.substring(idx)
        if (normalized.startsWith("uploads/")) return "/$normalized"
        return null
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
