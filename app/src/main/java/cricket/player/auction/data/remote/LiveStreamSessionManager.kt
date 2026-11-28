package cricket.player.auction.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.utils.gl.TranslateTo
import com.pedro.library.rtmp.RtmpCamera2
import com.pedro.library.view.OpenGlView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LiveStreamSessionManager {
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rtmpCamera: RtmpCamera2? = null
    private var monitorJob: Job? = null
    private var overlayFilter: ImageObjectFilterRender? = null
    private var lastOverlayLines: List<String> = emptyList()
    private var showOverlay: Boolean = true

    private val _activeBroadcast = MutableStateFlow<YouTubeBroadcastResult?>(null)
    val activeBroadcast: StateFlow<YouTubeBroadcastResult?> = _activeBroadcast.asStateFlow()

    private val _isRtmpStreaming = MutableStateFlow(false)
    val isRtmpStreaming: StateFlow<Boolean> = _isRtmpStreaming.asStateFlow()

    private val _isYouTubeLive = MutableStateFlow(false)
    val isYouTubeLive: StateFlow<Boolean> = _isYouTubeLive.asStateFlow()

    private val _streamStatusText = MutableStateFlow("DIRECT STREAM STUDIO")
    val streamStatusText: StateFlow<String> = _streamStatusText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _useFrontCamera = MutableStateFlow(false)
    val useFrontCamera: StateFlow<Boolean> = _useFrontCamera.asStateFlow()

    private val connectChecker = object : ConnectChecker {
        override fun onConnectionStarted(url: String) {
            mainHandler.post { _streamStatusText.value = "Connecting RTMP to YouTube..." }
        }

        override fun onConnectionSuccess() {
            mainHandler.post {
                _isRtmpStreaming.value = true
                _errorMessage.value = null
                if (!_isYouTubeLive.value) {
                    _streamStatusText.value = "RTMP Connected • Going LIVE on YouTube..."
                }
            }
        }

        override fun onConnectionFailed(reason: String) {
            mainHandler.post {
                _isRtmpStreaming.value = false
                _isYouTubeLive.value = false
                _errorMessage.value = "RTMP failed: $reason"
                _streamStatusText.value = "RTMP Connection Failed"
                runCatching { rtmpCamera?.stopStream() }
            }
        }

        override fun onNewBitrate(bitrate: Long) {}

        override fun onDisconnect() {
            mainHandler.post {
                _isRtmpStreaming.value = false
                _isYouTubeLive.value = false
                if (_activeBroadcast.value == null) {
                    _streamStatusText.value = "DIRECT STREAM STUDIO"
                }
            }
        }

        override fun onAuthError() {
            mainHandler.post { _errorMessage.value = "YouTube RTMP Auth Error" }
        }

        override fun onAuthSuccess() {}
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setStatusText(text: String) {
        _streamStatusText.value = text
    }

    fun setError(text: String?) {
        _errorMessage.value = text
    }

    fun setBroadcastResult(res: YouTubeBroadcastResult?) {
        _activeBroadcast.value = res
    }

    fun updateLiveState(isLive: Boolean, statusText: String) {
        _isYouTubeLive.value = isLive
        _streamStatusText.value = statusText
    }

    fun setMonitorJob(job: Job?) {
        monitorJob?.cancel()
        monitorJob = job
    }

    fun attachOpenGlView(openGlView: OpenGlView, context: Context) {
        val existing = rtmpCamera
        if (existing != null) {
            runCatching {
                existing.replaceView(openGlView)
                if (!existing.isOnPreview) {
                    existing.startPreview(
                        if (_useFrontCamera.value) CameraHelper.Facing.FRONT else CameraHelper.Facing.BACK,
                        1280,
                        720
                    )
                }
                overlayFilter = null
                applyOverlayToCamera(existing)
                mainHandler.postDelayed({ applyOverlayToCamera(existing) }, 200L)
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Failed to attach camera preview"
            }
            return
        }

        val cam = RtmpCamera2(openGlView, connectChecker)
        rtmpCamera = cam
        runCatching {
            if (!cam.isOnPreview) {
                cam.startPreview(
                    if (_useFrontCamera.value) CameraHelper.Facing.FRONT else CameraHelper.Facing.BACK,
                    1280,
                    720
                )
            }
            overlayFilter = null
            applyOverlayToCamera(cam)
            mainHandler.postDelayed({ applyOverlayToCamera(cam) }, 200L)
        }.onFailure { err ->
            _errorMessage.value = err.localizedMessage ?: "Camera preview failed"
        }
    }

    fun detachOpenGlView(context: Context) {
        val cam = rtmpCamera ?: return
        if (cam.isStreaming) {
            // Keep RTMP stream + OpenGL score overlay running offscreen in background!
            runCatching {
                cam.replaceView(context.applicationContext)
                overlayFilter = null
                applyOverlayToCamera(cam)
                mainHandler.postDelayed({ applyOverlayToCamera(cam) }, 200L)
            }
        } else {
            runCatching {
                if (cam.isOnPreview) cam.stopPreview()
            }
            rtmpCamera = null
            overlayFilter = null
        }
    }

    fun startRtmpStream(context: Context, res: YouTubeBroadcastResult, hasAudioPermission: Boolean): Boolean {
        val cam = rtmpCamera
        if (cam == null || res.ingestionAddress.isBlank() || res.streamKey.isBlank()) {
            _errorMessage.value = "Camera or RTMP ingestion info not ready."
            return false
        }
        return try {
            if (!cam.isStreaming) {
                if (hasAudioPermission) {
                    runCatching {
                        if (!cam.prepareAudio(128 * 1000, 44100, true)) {
                            cam.prepareAudio()
                        }
                    }
                }
                val rotation = runCatching { CameraHelper.getCameraOrientation(context) }.getOrDefault(0)
                val videoOk = cam.prepareVideo(1280, 720, 30, 2500 * 1000, rotation) ||
                    cam.prepareVideo(640, 480, 30, 1200 * 1000, rotation)
                if (!videoOk) {
                    _errorMessage.value = "Could not initialize camera video encoder for RTMP stream."
                    return false
                }
                val base = res.ingestionAddress.trimEnd('/')
                cam.startStream("$base/${res.streamKey}")
            }
            _activeBroadcast.value = res
            _isRtmpStreaming.value = true
            _streamStatusText.value = "Connecting RTMP to YouTube..."
            applyOverlayToCamera(cam)
            true
        } catch (e: Exception) {
            _errorMessage.value = "RTMP start error: ${e.localizedMessage}"
            false
        }
    }

    fun switchCamera() {
        _useFrontCamera.value = !_useFrontCamera.value
        runCatching { rtmpCamera?.switchCamera() }
    }

    fun stopStreamAndRelease(stopPreviewIfIdle: Boolean = false) {
        monitorJob?.cancel()
        monitorJob = null
        val cam = rtmpCamera
        if (cam != null) {
            runCatching {
                if (cam.isStreaming) cam.stopStream()
                if (stopPreviewIfIdle && cam.isOnPreview) cam.stopPreview()
            }
            if (stopPreviewIfIdle) {
                rtmpCamera = null
                overlayFilter = null
            }
        }
        _isRtmpStreaming.value = false
        _isYouTubeLive.value = false
        _activeBroadcast.value = null
        _streamStatusText.value = "DIRECT STREAM STUDIO"
    }

    fun updateOverlay(lines: List<String>, visible: Boolean = showOverlay) {
        lastOverlayLines = lines
        showOverlay = visible
        val cam = rtmpCamera ?: return
        applyOverlayToCamera(cam)
    }

    private fun applyOverlayToCamera(cam: RtmpCamera2) {
        runCatching {
            val filter = overlayFilter ?: ImageObjectFilterRender().also {
                overlayFilter = it
                cam.glInterface.setFilter(it)
            }
            val bmp = if (showOverlay && lastOverlayLines.isNotEmpty()) {
                buildBroadcastOverlayBitmap(lastOverlayLines)
            } else {
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }
            filter.setImage(bmp)
            filter.setScale(92f, 17f)
            filter.setPosition(TranslateTo.BOTTOM)
        }
    }

    private fun buildBroadcastOverlayBitmap(lines: List<String>): Bitmap {
        val width = 1080
        val height = 190
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        if (lines.isEmpty()) return bmp

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(232, 15, 23, 42)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(245, 158, 11)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val rect = RectF(6f, 6f, width - 6f, height - 6f)
        canvas.drawRoundRect(rect, 22f, 22f, bgPaint)
        canvas.drawRoundRect(rect, 22f, 22f, borderPaint)

        val liveBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(220, 38, 38)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(24f, 22f, 128f, 68f), 12f, 12f, liveBg)

        val liveText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("LIVE", 76f, 54f, liveText)

        val line1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val line2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(250, 204, 21)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val line3Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(148, 163, 184)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText(lines.getOrNull(0).orEmpty().take(58), 148f, 58f, line1Paint)
        if (lines.size > 1) {
            canvas.drawText(lines[1].take(72), 28f, 114f, line2Paint)
        }
        if (lines.size > 2) {
            canvas.drawText(lines[2].take(80), 28f, 158f, line3Paint)
        }
        return bmp
    }
}
