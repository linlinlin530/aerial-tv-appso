package app.orchard.tv.ui.home
import org.junit.Assert.*
import org.junit.Test
class ArticleConfirmTest {
    @Test fun requiresTwoPressesWithinThreeSeconds() {
        val c = ArticleConfirm()
        assertFalse(c.press("a", 1, 1000))
        assertTrue(c.press("a", 1, 4000))
        assertFalse(c.press("a", 1, 4500))
    }
    @Test fun timeoutRearmsInsteadOfOpening() {
        val c = ArticleConfirm()
        assertFalse(c.press("a", 1, 1000))
        assertFalse(c.press("a", 1, 4001))
        assertTrue(c.press("a", 1, 4500))
    }
    @Test fun changedArticleSessionAndSkipResetConfirmation() {
        val c = ArticleConfirm()
        assertFalse(c.press("a", 1, 1000))
        assertFalse(c.press("b", 1, 1100))
        assertFalse(c.press("b", 2, 1200))
        c.reset()
        assertFalse(c.press("b", 2, 1300))
        assertTrue(c.press("b", 2, 1400))
    }
}
