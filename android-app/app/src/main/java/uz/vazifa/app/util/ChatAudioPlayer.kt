package uz.vazifa.app.util

import android.media.MediaPlayer
import android.media.PlaybackParams

/** Telegram uslubidagi ovozli xabar pleyeri (tezlikni boshqarish bilan) */
class ChatAudioPlayer {
    private var player: MediaPlayer? = null
    var currentUrl: String? = null
        private set

    fun toggle(
        url: String,
        speed: Float,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
    ): Boolean {
        if (currentUrl == url && player?.isPlaying == true) {
            pause()
            return false
        }
        if (currentUrl == url && player != null) {
            play(speed)
            return true
        }
        release()
        currentUrl = url
        player = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener {
                setSpeed(speed)
                start()
            }
            setOnCompletionListener {
                onProgress(1f)
                onComplete()
                releaseInternal()
            }
            prepareAsync()
        }
        return true
    }

    fun setSpeed(speed: Float) {
        val p = player ?: return
        runCatching {
            val wasPlaying = p.isPlaying
            p.playbackParams = (p.playbackParams ?: PlaybackParams()).setSpeed(speed)
            if (!wasPlaying) p.pause()
        }
    }

    fun positionFraction(): Float {
        val p = player ?: return 0f
        val dur = p.duration.takeIf { it > 0 } ?: return 0f
        return (p.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
    }

    fun isPlaying(url: String): Boolean = currentUrl == url && player?.isPlaying == true

    private fun play(speed: Float) {
        player?.let {
            it.playbackParams = it.playbackParams.setSpeed(speed)
            it.start()
        }
    }

    fun pause() {
        runCatching { player?.pause() }
    }

    private fun releaseInternal() {
        player?.release()
        player = null
        currentUrl = null
    }

    fun release() {
        runCatching { player?.release() }
        player = null
        currentUrl = null
    }
}
