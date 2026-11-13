package app.orchard.tv.wallpaper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23])
class IfanrArticlesTest {
    private fun item(title: String = "封面 &amp; Title", link: String = "https://www.ifanr.com/123?utm_source=rss", image: String = "https://s3.ifanr.com/cover.jpg") =
        "<item><title>$title</title><link>${link.replace("&", "&amp;")}</link><image>$image</image><description><![CDATA[<img src='wrong.jpg'>]]></description></item>"
    @Test fun usesCoverAndCanonicalLinkInFeedOrder() {
        val rows = IfanrArticles.parse("<rss><channel>" + item() + item(link="https://www.ifanr.com/124") + "</channel></rss>")
        assertEquals(2, rows.size)
        assertEquals("封面 & Title", rows[0].title)
        assertEquals("https://www.ifanr.com/123", rows[0].link)
        assertEquals("https://s3.ifanr.com/cover.jpg", rows[0].image)
    }
    @Test fun skipsMissingCoversDuplicatesAndUnsafeUrls() {
        val rows = IfanrArticles.parse("<rss><channel>" + item() + item() + item(image="") + item(link="https://evil.test/123") + item(image="https://s3.ifanr.com.evil.test/image.jpg") + item(link="javascript:alert(1)") + "</channel></rss>")
        assertEquals(1, rows.size)
    }
    @Test fun rejectsDoctype() {
        assertThrows(Exception::class.java) { IfanrArticles.parse("<!DOCTYPE rss><rss/>") }
    }
}
