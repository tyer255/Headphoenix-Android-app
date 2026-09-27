package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Headless YouTube audio playback engine that integrates directly with Android's
 * background PlaybackService and MediaSession. Plays full-length YouTube media
 * descriptors returned by the spotify2.ai.studio backend API.
 */
class YouTubeAudioEngine(private val context: Context) {

    companion object {
        private const val TAG = "YouTubeAudioEngine"
    }

    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isEngineReady = false
    private var currentVideoId: String? = null
    private var isPlayingState = false
    private var pendingPlayAfterReady = false

    var onStateChangedListener: ((isPlaying: Boolean) -> Unit)? = null
    var onProgressListener: ((currentSec: Float, durationSec: Float) -> Unit)? = null
    var onEndedListener: (() -> Unit)? = null
    var onErrorListener: ((errorCode: Int) -> Unit)? = null

    init {
        mainHandler.post {
            initWebView()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        try {
            val wv = WebView(context.applicationContext)
            wv.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            val settings = wv.settings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "YouTube Player bridge page loaded")
                }
            }

            wv.webChromeClient = WebChromeClient()

            wv.addJavascriptInterface(object {
                @JavascriptInterface
                fun onPlayerReady() {
                    mainHandler.post {
                        Log.d(TAG, "YouTube IFrame Player ready")
                        isEngineReady = true
                        if (pendingPlayAfterReady) {
                            pendingPlayAfterReady = false
                            play()
                        }
                    }
                }

                @JavascriptInterface
                fun onPlayerStateChange(state: Int) {
                    mainHandler.post {
                        Log.d(TAG, "YouTube player state: $state")
                        when (state) {
                            1 -> { // PLAYING
                                isPlayingState = true
                                onStateChangedListener?.invoke(true)
                            }
                            2 -> { // PAUSED
                                isPlayingState = false
                                onStateChangedListener?.invoke(false)
                            }
                            0 -> { // ENDED
                                isPlayingState = false
                                onStateChangedListener?.invoke(false)
                                onEndedListener?.invoke()
                            }
                            3 -> { // BUFFERING
                                // Keep current playing state
                            }
                        }
                    }
                }

                @JavascriptInterface
                fun onPlayerProgress(currentTime: Float, duration: Float) {
                    mainHandler.post {
                        if (isPlayingState) {
                            onProgressListener?.invoke(currentTime, duration)
                        }
                    }
                }

                @JavascriptInterface
                fun onPlayerError(errorCode: Int) {
                    mainHandler.post {
                        Log.e(TAG, "YouTube player error: $errorCode")
                        onErrorListener?.invoke(errorCode)
                    }
                }
            }, "AndroidBridge")

            webView = wv
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize YouTube WebView engine", e)
        }
    }

    fun loadAndPlay(videoId: String, startSeconds: Float = 0f) {
        currentVideoId = videoId
        pendingPlayAfterReady = true
        isEngineReady = false

        mainHandler.post {
            try {
                val html = buildPlayerHtml(videoId, startSeconds)
                webView?.loadDataWithBaseURL("https://spotify2.ai.studio", html, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load YouTube video $videoId", e)
            }
        }
    }

    fun play() {
        mainHandler.post {
            if (isEngineReady) {
                webView?.evaluateJavascript("if (window.player && player.playVideo) { player.playVideo(); }", null)
            } else {
                pendingPlayAfterReady = true
            }
        }
    }

    fun pause() {
        mainHandler.post {
            webView?.evaluateJavascript("if (window.player && player.pauseVideo) { player.pauseVideo(); }", null)
        }
    }

    fun seekTo(seconds: Float) {
        mainHandler.post {
            webView?.evaluateJavascript("if (window.player && player.seekTo) { player.seekTo($seconds, true); }", null)
        }
    }

    fun setVolume(volume: Float) {
        val ytVolume = (volume.coerceIn(0f, 1f) * 100).toInt()
        mainHandler.post {
            webView?.evaluateJavascript("if (window.player && player.setVolume) { player.setVolume($ytVolume); }", null)
        }
    }

    fun stop() {
        mainHandler.post {
            isPlayingState = false
            currentVideoId = null
            webView?.evaluateJavascript("if (window.player && player.stopVideo) { player.stopVideo(); }", null)
            webView?.loadUrl("about:blank")
        }
    }

    fun isPlaying(): Boolean = isPlayingState

    private fun buildPlayerHtml(videoId: String, startSeconds: Float): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { margin: 0; padding: 0; background-color: #000000; overflow: hidden; }
                    #player { width: 100%; height: 100%; }
                </style>
                <script src="https://www.youtube.com/iframe_api"></script>
            </head>
            <body>
                <div id="player"></div>
                <script>
                    var player;
                    var progressInterval;

                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            height: '100%',
                            width: '100%',
                            videoId: '$videoId',
                            playerVars: {
                                'autoplay': 1,
                                'controls': 0,
                                'disablekb': 1,
                                'fs': 0,
                                'modestbranding': 1,
                                'playsinline': 1,
                                'rel': 0,
                                'start': Math.floor($startSeconds),
                                'origin': 'https://spotify2.ai.studio'
                            },
                            events: {
                                'onReady': onPlayerReady,
                                'onStateChange': onPlayerStateChange,
                                'onError': onPlayerError
                            }
                        });
                    }

                    function onPlayerReady(event) {
                        try {
                            event.target.playVideo();
                            if (window.AndroidBridge) {
                                window.AndroidBridge.onPlayerReady();
                            }
                            startProgressReporting();
                        } catch(e) {}
                    }

                    function onPlayerStateChange(event) {
                        try {
                            if (window.AndroidBridge) {
                                window.AndroidBridge.onPlayerStateChange(event.data);
                            }
                        } catch(e) {}
                    }

                    function onPlayerError(event) {
                        try {
                            if (window.AndroidBridge) {
                                window.AndroidBridge.onPlayerError(event.data);
                            }
                        } catch(e) {}
                    }

                    function startProgressReporting() {
                        if (progressInterval) clearInterval(progressInterval);
                        progressInterval = setInterval(function() {
                            try {
                                if (player && typeof player.getCurrentTime === 'function' && typeof player.getDuration === 'function') {
                                    var curr = player.getCurrentTime() || 0;
                                    var dur = player.getDuration() || 0;
                                    if (window.AndroidBridge) {
                                        window.AndroidBridge.onPlayerProgress(curr, dur);
                                    }
                                }
                            } catch(e) {}
                        }, 250);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
