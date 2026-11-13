package app.orchard.tv.wallpaper

import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], application = Application::class)
class ArticleReaderTest {
    @Test fun opensOriginalArticleInInternalActivity() {
        val link = "https://www.ifanr.com/1689249"
        val intent = ArticleReaderActivity.intent(RuntimeEnvironment.getApplication(), link)
        assertEquals(link, intent.dataString)
        assertEquals(ArticleReaderActivity::class.java.name, intent.component!!.className)
    }
    @Test fun rejectsExternalAndUnsafeNavigation() {
        for (url in listOf("http://www.ifanr.com/123", "https://www.ifanr.com.evil.test/123", "file:///tmp/123", "javascript:alert(1)", "https://www.ifanr.com:8443/123", "https://user@www.ifanr.com/123")) {
            assertFalse(url, ArticleReaderActivity.allowedArticle(url))
            assertThrows(IllegalArgumentException::class.java) { ArticleReaderActivity.intent(RuntimeEnvironment.getApplication(), url) }
        }
    }
    @Test fun httpsTransportOnlyHandlesPublisherAssets() {
        assertTrue(ArticleWebTransport.allowed("https://s3.ifanr.com/a.jpg"))
        assertFalse(ArticleWebTransport.allowed("https://s3.ifanr.com.evil.test/a.jpg"))
        assertFalse(ArticleWebTransport.allowed("http://s3.ifanr.com/a.jpg"))
        assertFalse(ArticleWebTransport.allowed("https://127.0.0.1/a"))
    }
}
