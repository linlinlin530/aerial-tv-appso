package app.orchard.tv.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

/** Latest articles from ifanr's own RSS feed; bounded local cache, no tracking or third-party SDK. */
object IfanrArticles {
    data class Article(val title: String, val link: String, val image: String)
    const val REFRESH_MS = 2 * 60 * 60_000L
    private val refreshLock = Mutex()
    private const val LIMIT = 16
    @Volatile var session = 0L
    @Volatile var current: Article? = null
    @Volatile var status = "idle"
        private set
    private fun dir(context: Context) = File(context.filesDir, "ifanr").apply { mkdirs() }
    private fun allowed(raw: String, article: Boolean = false): Boolean = runCatching {
        val u = URI(raw)
        u.scheme == "https" && u.userInfo == null && (u.port == -1 || u.port == 443) &&
            if (article) u.host in setOf("ifanr.com", "www.ifanr.com") && Regex("/[0-9]+/?").matches(u.path)
            else u.host in setOf("ifanr.com", "www.ifanr.com", "s3.ifanr.com", "images.ifanr.cn")
    }.getOrDefault(false)

    fun parse(xml: String): List<Article> {
        require(!xml.contains("<!DOCTYPE", ignoreCase = true))
        val parser = Xml.newPullParser()
        parser.setInput(xml.reader())
        val result = mutableListOf<Article>()
        var itemDepth = -1
        var title = ""; var link = ""; var image = ""
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                if (parser.name == "item") { itemDepth = parser.depth; title = ""; link = ""; image = "" }
                else if (itemDepth >= 0 && parser.depth == itemDepth + 1) when (parser.name) {
                    "title" -> title = parser.nextText().trim().take(400)
                    "link" -> link = parser.nextText().trim().substringBefore('?').substringBefore('#')
                    "image" -> image = parser.nextText().trim().replace("http://", "https://")
                }
            } else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "item") {
                if (title.isNotEmpty() && allowed(link, true) && allowed(image) && result.none { it.link == link }) result += Article(title, link, image)
                itemDepth = -1
                if (result.size >= LIMIT) break
            }
            parser.next()
        }
        return result
    }

    private fun download(context: Context, raw: String, max: Int): ByteArray {
        var url = raw
        repeat(4) {
            require(allowed(url))
            val c = URL(url).openConnection() as HttpURLConnection
            try {
                app.orchard.tv.update.LegacyUpdateTls.configure(context, c)
                c.connectTimeout = 8000; c.readTimeout = 10000; c.instanceFollowRedirects = false
                c.setRequestProperty("User-Agent", "AerialTV/1.96 ifanr-screensaver")
                if (c.responseCode in 300..399) { url = URL(URL(url), c.getHeaderField("Location")).toString() }
                else {
                    check(c.responseCode == 200)
                    require(c.contentLength <= max)
                    return c.inputStream.use { input ->
                        val out = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val n = input.read(buffer); if (n < 0) break
                            require(out.size() + n <= max)
                            out.write(buffer, 0, n)
                        }
                        out.toByteArray()
                    }
                }
            } finally { c.disconnect() }
        }
        error("Too many redirects")
    }

    fun cached(context: Context): List<Article> = runCatching {
        val a = JSONArray(File(dir(context), "articles.json").readText())
        (0 until a.length()).map { a.getJSONObject(it).let { o -> Article(o.getString("title"), o.getString("link"), o.getString("image")) } }
            .filter { allowed(it.link, true) && allowed(it.image) }.take(LIMIT)
    }.getOrDefault(emptyList())

    suspend fun refresh(context: Context): List<Article> = refreshLock.withLock { withContext(Dispatchers.IO) {
        val cache = File(dir(context), "articles.json")
        val old = cached(context)
        if (old.isNotEmpty() && System.currentTimeMillis() - cache.lastModified() in 0 until REFRESH_MS) return@withContext old
        try {
            val fresh = parse(download(context, "https://www.ifanr.com/feed", 2 * 1024 * 1024).toString(Charsets.UTF_8))
            ensureActive()
            check(fresh.isNotEmpty())
            val data = JSONArray().apply { fresh.forEach { put(JSONObject().put("title", it.title).put("link", it.link).put("image", it.image)) } }
            val part = File(dir(context), "articles.tmp"); part.writeText(data.toString()); check(part.renameTo(cache))
            status = "fresh: ${fresh.size}"
            fresh
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            status = "cached: ${old.size}; ${e.javaClass.simpleName}: ${e.message}"
            old
        }
    }

    }

    suspend fun picture(context: Context, article: Article): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val name = MessageDigest.getInstance("SHA-256").digest(article.image.toByteArray()).joinToString("") { "%02x".format(it) }
            val file = File(dir(context), "$name.img")
            if (!file.exists()) {
                val part = File(dir(context), "$name.part")
                try { part.writeBytes(download(context, article.image, 8 * 1024 * 1024)); check(part.renameTo(file)) } finally { part.delete() }
            }
            file.setLastModified(System.currentTimeMillis())
            val bitmap = Images.decodeWhole(file, Wallpapers.screenW.coerceIn(960, 1920), Wallpapers.screenH.coerceIn(540, 1080))
            if (bitmap == null) file.delete()
            var bytes = 0L
            dir(context).listFiles()?.filter { it.extension == "img" }?.sortedByDescending { it.lastModified() }?.forEach {
                bytes += it.length(); if (bytes > 48L * 1024 * 1024 && it != file) it.delete()
            }
            bitmap
        }.getOrElse { status = "image: ${it.javaClass.simpleName}: ${it.message}"; null }
    }
}
