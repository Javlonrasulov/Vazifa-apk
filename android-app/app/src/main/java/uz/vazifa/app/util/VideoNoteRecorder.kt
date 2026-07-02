package uz.vazifa.app.util

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VideoNoteRecorder(private val context: Context) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var outputFile: File? = null
    private var previewView: PreviewView? = null
    private var lifecycleOwner: LifecycleOwner? = null

    var useFrontCamera: Boolean = true
        private set

    private val mainExecutor: Executor get() = ContextCompat.getMainExecutor(context)

    suspend fun bindPreview(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        this.lifecycleOwner = lifecycleOwner
        this.previewView = previewView
        val provider = getCameraProvider()
        cameraProvider = provider
        bindUseCases(provider, lifecycleOwner, previewView)
    }

    fun flipCamera() {
        useFrontCamera = !useFrontCamera
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return
        val view = previewView ?: return
        bindUseCases(provider, owner, view)
    }

    fun start(): File {
        cancelRecordingOnly()
        val file = File(context.cacheDir, "video_note_${System.currentTimeMillis()}.mp4")
        outputFile = file
        val capture = videoCapture ?: error("Camera not ready")
        val options = FileOutputOptions.Builder(file).build()
        activeRecording = capture.output
            .prepareRecording(context, options)
            .withAudioEnabled()
            .start(mainExecutor) { }
        return file
    }

    fun stop(): File? {
        val recording = activeRecording
        activeRecording = null
        return try {
            recording?.stop()
            outputFile?.takeIf { it.exists() && it.length() > 0 }
        } catch (_: Exception) {
            outputFile?.delete()
            null
        }
    }

    fun cancel() {
        cancelRecordingOnly()
        outputFile?.delete()
        outputFile = null
    }

    fun release() {
        cancel()
        cameraProvider?.unbindAll()
        cameraProvider = null
        videoCapture = null
        previewView = null
        lifecycleOwner = null
    }

    private fun cancelRecordingOnly() {
        runCatching { activeRecording?.stop() }
        activeRecording = null
    }

    private fun bindUseCases(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ) {
        provider.unbindAll()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.SD))
            .build()
        val capture = VideoCapture.withOutput(recorder)
        videoCapture = capture
        val selector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cont.resume(future.get())
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }, mainExecutor)
    }
}
