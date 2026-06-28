package uz.vazifa.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

/** Telegram uslubidagi ovozli xabar pleyeri (tezlikni boshqarish bilan) */
class ChatAudioPlayer {
    private var player: MediaPlayer? = null
    var currentSource: String? = null
        private set

    suspend fun toggleLocal(
        file: File,
        speed: Float,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: () -> Unit,
    ): Boolean {
        val key = "file:${file.absolutePath}"
        if (currentSource == key && player?.isPlaying == true) {
            pause()
            return false
        }
        if (currentSource == key && player != null) {
            resume(speed)
            return true
        }
        release()
        currentSource = key
        return startPlayer(file.absolutePath, speed, onProgress, onComplete, onError)
    }

    suspend fun toggleRemote(
        context: Context,
        remoteUrl: String,
        speed: Float,
        authToken: String? = null,
        okHttpClient: OkHttpClient? = null,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: () -> Unit,
    ): Boolean {
        val local = cacheRemote(context, remoteUrl, authToken, okHttpClient) ?: run {
            onError()
            return false
        }
        return toggleLocal(local, speed, onProgress, onComplete, onError)
    }

    private suspend fun cacheRemote(
        context: Context,
        remoteUrl: String,
        authToken: String? = null,
        okHttpClient: OkHttpClient? = null,
    ): File? = withContext(Dispatchers.IO) {
        val name = remoteUrl.substringAfterLast('/').ifBlank { remoteUrl.hashCode().toString() }
        val cacheFile = File(context.cacheDir, "voice_play_$name")
        if (cacheFile.exists() && cacheFile.length() <= 512) cacheFile.delete()
        if (cacheFile.exists() && cacheFile.length() > 512) return@withContext cacheFile

        if (okHttpClient != null) {
            runCatching {
                val response = okHttpClient.newCall(Request.Builder().url(remoteUrl).get().build()).execute()
                response.use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    resp.body?.byteStream()?.use { input ->
                        cacheFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("empty body")
                }
                cacheFile.takeIf { it.length() > 512 }
            }.getOrNull()?.let { return@withContext it }
        }

        runCatching {
            val conn = URL(remoteUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 12_000
            conn.readTimeout = 20_000
            conn.requestMethod = "GET"
            if (!authToken.isNullOrBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $authToken")
            }
            conn.connect()
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            conn.inputStream.use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
            cacheFile.takeIf { it.length() > 512 }
        }.getOrNull()
    }

    private suspend fun startPlayer(
        source: String,
        speed: Float,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: () -> Unit,
    ): Boolean = suspendCancellableCoroutine { cont ->
        val mp = MediaPlayer()
        player = mp
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        mp.setDataSource(source)
        mp.setOnPreparedListener { prepared ->
            prepared.start()
            applySpeed(prepared, speed)
            if (cont.isActive) cont.resume(true)
        }
        mp.setOnCompletionListener {
            onProgress(1f)
            onComplete()
            releaseInternal()
        }
        mp.setOnErrorListener { _, _, _ ->
            onError()
            releaseInternal()
            if (cont.isActive) cont.resume(false)
            true
        }
        cont.invokeOnCancellation {
            releaseInternal()
        }
        runCatching { mp.prepareAsync() }.onFailure {
            onError()
            releaseInternal()
            if (cont.isActive) cont.resume(false)
        }
    }

    fun setSpeed(speed: Float) {
        player?.let { applySpeed(it, speed) }
    }

    private fun applySpeed(p: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        runCatching {
            p.playbackParams = (p.playbackParams ?: PlaybackParams()).setSpeed(speed)
        }
    }

    fun positionFraction(): Float {
        val p = player ?: return 0f
        val dur = p.duration.takeIf { it > 0 } ?: return 0f
        return (p.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
    }

    fun isPlaying(sourceKey: String): Boolean = currentSource == sourceKey && player?.isPlaying == true

    private fun resume(speed: Float) {
        player?.let {
            applySpeed(it, speed)
            it.start()
        }
    }

    fun pause() {
        runCatching { player?.pause() }
    }

    private fun releaseInternal() {
        player?.release()
        player = null
        currentSource = null
    }

    fun release() {
        runCatching { player?.release() }
        player = null
        currentSource = null
    }
}
