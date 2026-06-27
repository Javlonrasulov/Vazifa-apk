package uz.vazifa.app.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(): File {
        cancel()
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        outputFile = file
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(96_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        mediaRecorder = recorder
        return file
    }

    fun amplitude(): Int = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    fun stop(): File? {
        val file = outputFile
        return try {
            mediaRecorder?.stop()
            file?.takeIf { it.exists() && it.length() > 0 }
        } catch (_: Exception) {
            file?.delete()
            null
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }

    fun cancel() {
        runCatching { mediaRecorder?.stop() }
        mediaRecorder?.release()
        mediaRecorder = null
        outputFile?.delete()
        outputFile = null
    }
}
