package app.orchard.tv.wallpaper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.*
import android.widget.LinearLayout
import android.widget.TextView
import app.orchard.tv.R
import app.orchard.tv.ui.theme.DotFonts
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** The publisher's original page, rendered by the TV's existing system WebView. */
class ArticleReaderActivity : Activity() {
    private var web: WebView? = null
    private lateinit var message: TextView
    companion object {
        const val DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Safari/537.36"
        @Volatile var status = "idle"
        @Volatile var scrollPosition = 0
        fun allowedArticle(raw: String): Boolean = runCatching {
            val u = URI(raw)
            u.scheme == "https" && u.userInfo == null && (u.port == -1 || u.port == 443) &&
                u.host in setOf("www.ifanr.com", "ifanr.com") && Regex("/[0-9]+/?").matches(u.path)
        }.getOrDefault(false)
        fun intent(context: Context, link: String): Intent {
            require(allowedArticle(link))
            return Intent(context, ArticleReaderActivity::class.java).setData(Uri.parse(link))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val link = intent.dataString.orEmpty()
        if (!allowedArticle(link) || app.orchard.tv.settings.Restricted.active) { finish(); return }
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK) }
        message = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 18f; typeface = DotFonts.native
            setPadding(24, 12, 24, 12); setText(R.string.article_loading)
        }
        root.addView(message); setContentView(root)
        status = "loading"
        try {
            val view = WebView(this)
            web = view
            view.settings.apply {
                userAgentString = DESKTOP_UA
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
                useWideViewPort = true
                loadWithOverviewMode = true
                textZoom = 100
            }
            view.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = !allowedArticle(url)
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = !allowedArticle(request.url.toString())
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    val url = request.url.toString()
                    if (request.method != "GET" || !ArticleWebTransport.allowed(url)) return null
                    return try { ArticleWebTransport.fetch(this@ArticleReaderActivity, url) }
                    catch (e: Exception) {
                        if (request.isForMainFrame) runOnUiThread { status = "error: ${e.javaClass.simpleName}"; message.setText(R.string.article_failed) }
                        WebResourceResponse("text/plain", "UTF-8", 502, "Load failed", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                    }
                }
                override fun onPageFinished(view: WebView, url: String) {
                    if (!status.startsWith("error")) { status = "loaded-desktop"; message.setText(R.string.article_keys) }
                }
                override fun onReceivedError(view: WebView, code: Int, description: String, failingUrl: String) {
                    status = "error: $code"; message.setText(R.string.article_failed)
                }
                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: android.net.http.SslError) {
                    handler.cancel()
                    if (allowedArticle(error.url)) { status = "error: TLS"; message.setText(R.string.article_failed) }
                }
            }
            root.addView(view, LinearLayout.LayoutParams(-1, 0, 1f))
            view.requestFocus(); view.loadUrl(link)
        } catch (e: Exception) { status = "unavailable: ${e.javaClass.simpleName}"; message.setText(R.string.article_unavailable) }
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP || event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN) web?.let {
                it.scrollBy(0, (it.height * 0.55f).toInt() * if (event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) 1 else -1)
                scrollPosition = it.scrollY
            }
            return true
        }
        if (status.startsWith("error") && event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (event.action == KeyEvent.ACTION_UP) { status = "loading"; message.setText(R.string.article_loading); web?.reload() }
            return true
        }
        return super.dispatchKeyEvent(event)
    }
    override fun onDestroy() {
        web?.apply { stopLoading(); (parent as? android.view.ViewGroup)?.removeView(this); destroy() }
        web = null
        super.onDestroy()
    }
}

/** Android 6 WebView lacks modern roots; use the app's validated HTTPS path for ifanr assets. */
object ArticleWebTransport {
    fun allowed(raw: String): Boolean = runCatching {
        val u = URI(raw)
        u.scheme == "https" && u.userInfo == null && (u.port == -1 || u.port == 443) &&
            u.host in setOf("www.ifanr.com", "ifanr.com", "s3.ifanr.com", "images.ifanr.cn")
    }.getOrDefault(false)
    fun fetch(context: Context, raw: String): WebResourceResponse {
        var url = raw
        repeat(4) {
            require(allowed(url))
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                app.orchard.tv.update.LegacyUpdateTls.configure(context, connection)
                connection.connectTimeout = 8000; connection.readTimeout = 10000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", ArticleReaderActivity.DESKTOP_UA)
                if (connection.responseCode in 300..399) url = URL(URL(url), connection.getHeaderField("Location")).toString()
                else {
                    check(connection.responseCode == 200)
                    val limit = 8 * 1024 * 1024
                    require(connection.contentLength <= limit)
                    val bytes = connection.inputStream.use { input ->
                        val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                        while (true) { val n = input.read(buffer); if (n < 0) break; require(out.size() + n <= limit); out.write(buffer, 0, n) }
                        out.toByteArray()
                    }
                    val type = connection.contentType.orEmpty().substringBefore(';').ifBlank { "application/octet-stream" }
                    val body = if (type == "text/css") LegacyArticleCss.flattenLayers(bytes.toString(Charsets.UTF_8)).toByteArray(Charsets.UTF_8) else bytes
                    return WebResourceResponse(type, "UTF-8", ByteArrayInputStream(body))
                }
            } finally { connection.disconnect() }
        }
        error("Too many redirects")
    }
}
