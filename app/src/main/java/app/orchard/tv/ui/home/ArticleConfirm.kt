package app.orchard.tv.ui.home

/** Two completed remote presses on the same displayed article, at most three seconds apart. */
class ArticleConfirm {
    private var article: String? = null
    private var session = -1L
    private var at = 0L
    fun reset() { article = null }
    fun press(link: String, currentSession: Long, now: Long): Boolean {
        val confirmed = article == link && session == currentSession && now - at in 0..3000
        article = if (confirmed) null else link
        session = currentSession
        at = now
        return confirmed
    }
}
