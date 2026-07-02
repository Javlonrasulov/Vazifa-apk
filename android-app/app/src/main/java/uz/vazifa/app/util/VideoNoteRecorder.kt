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
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
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
    private var finalizeDeferred: CompletableDeferred<File?>? = null
    private val segmentFiles = mutableListOf<File>()

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

    suspend fun flipCamera() {
        if (activeRecording != null) {
            awaitSegment()?.let { segmentFiles.add(it) }
            useFrontCamera = !useFrontCamera
            rebindCamera()
            startSegment()
            return
        }
        useFrontCamera = !useFrontCamera
        rebindCamera()
    }

    fun start(): File {
        segmentFiles.clear()
        return startSegment()
    }

    private fun startSegment(): File {
        cancelRecordingOnly()
        val file = File(context.cacheDir, "video_note_${System.currentTimeMillis()}.mp4")
        outputFile = file
        val capture = videoCapture ?: error("Camera not ready")
        val options = FileOutputOptions.Builder(file).build()
        finalizeDeferred = CompletableDeferred()
        activeRecording = capture.output
            .prepareRecording(context, options)
            .withAudioEnabled()
            .start(mainExecutor) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    val deferred = finalizeDeferred
                    finalizeDeferred = null
                    if (event.hasError()) {
                        outputFile?.delete()
                        deferred?.complete(null)
                    } else {
                        deferred?.complete(outputFile?.takeIf { it.exists() && it.length() > 512 })
                    }
                }
            }
        return file
    }

    suspend fun stop(): File? {
        awaitSegment()?.let { segmentFiles.add(it) }
        val segments = segmentFiles.toList()
        segmentFiles.clear()
        if (segments.isEmpty()) return null
        if (segments.size == 1) return segments.first()
        val merged = File(context.cacheDir, "video_note_merged_${System.currentTimeMillis()}.mp4")
        val result = VideoNoteMerger.merge(segments, merged)
        segments.forEach { if (it != result) it.delete() }
        return result
    }

    private suspend fun awaitSegment(): File? {
        val recording = activeRecording ?: return null
        val deferred = finalizeDeferred ?: CompletableDeferred<File?>().also { finalizeDeferred = it }
        activeRecording = null
        recording.stop()
        return withTimeoutOrNull(30_000) { deferred.await() }
    }

    fun cancel() {
        cancelRecordingOnly()
        finalizeDeferred?.complete(null)
        finalizeDeferred = null
        outputFile?.delete()
        outputFile = null
        segmentFiles.forEach { it.delete() }
        segmentFiles.clear()
    }

    fun releaseCamera() {
        cancelRecordingOnly()
        cameraProvider?.unbindAll()
        cameraProvider = null
        videoCapture = null
        previewView = null
        lifecycleOwner = null
    }

    fun release() {
        cancel()
        releaseCamera()
    }

    private fun cancelRecordingOnly() {
        runCatching { activeRecording?.stop() }
        activeRecording = null
    }

    private fun rebindCamera() {
        val provider = cameraProvider ?: return
        val owner = lifecycleOwner ?: return
        val view = previewView ?: return
        bindUseCases(provider, owner, view)
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
