package uz.vazifa.app.util

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

object VideoNoteMerger {
    fun merge(files: List<File>, output: File): File? {
        val valid = files.filter { it.exists() && it.length() > 512 }
        if (valid.isEmpty()) return null
        if (valid.size == 1) return valid.first()

        return runCatching {
            val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerVideoTrack = -1
            var muxerAudioTrack = -1
            var offsetUs = 0L
            var started = false

            for (file in valid) {
                val extractor = MediaExtractor()
                extractor.setDataSource(file.absolutePath)
                var videoExtTrack = -1
                var audioExtTrack = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    when {
                        mime.startsWith("video/") -> videoExtTrack = i
                        mime.startsWith("audio/") -> audioExtTrack = i
                    }
                }
                if (videoExtTrack >= 0 && muxerVideoTrack < 0) {
                    muxerVideoTrack = muxer.addTrack(extractor.getTrackFormat(videoExtTrack))
                }
                if (audioExtTrack >= 0 && muxerAudioTrack < 0) {
                    muxerAudioTrack = muxer.addTrack(extractor.getTrackFormat(audioExtTrack))
                }
                if (!started && (muxerVideoTrack >= 0 || muxerAudioTrack >= 0)) {
                    muxer.start()
                    started = true
                }
                var segmentEndUs = offsetUs
                if (videoExtTrack >= 0 && muxerVideoTrack >= 0) {
                    segmentEndUs = maxOf(segmentEndUs, copyTrack(extractor, videoExtTrack, muxer, muxerVideoTrack, offsetUs))
                }
                if (audioExtTrack >= 0 && muxerAudioTrack >= 0) {
                    segmentEndUs = maxOf(segmentEndUs, copyTrack(extractor, audioExtTrack, muxer, muxerAudioTrack, offsetUs))
                }
                offsetUs = segmentEndUs + 50_000
                extractor.release()
            }

            if (!started) return@runCatching null
            muxer.stop()
            muxer.release()
            output.takeIf { it.exists() && it.length() > 512 }
        }.getOrNull()
    }

    private fun copyTrack(
        extractor: MediaExtractor,
        extTrack: Int,
        muxer: MediaMuxer,
        muxTrack: Int,
        offsetUs: Long,
    ): Long {
        extractor.selectTrack(extTrack)
        val buffer = ByteBuffer.allocate(512 * 1024)
        val info = android.media.MediaCodec.BufferInfo()
        var lastUs = offsetUs
        while (true) {
            info.offset = 0
            info.size = extractor.readSampleData(buffer, 0)
            if (info.size < 0) break
            info.presentationTimeUs = extractor.sampleTime + offsetUs
            info.flags = extractor.sampleFlags
            muxer.writeSampleData(muxTrack, buffer, info)
            lastUs = info.presentationTimeUs
            extractor.advance()
        }
        extractor.unselectTrack(extTrack)
        return lastUs
    }
}
