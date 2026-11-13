package app.orchard.tv.wallpaper

/** Keep the publisher's CSS rules visible on WebView 66, which ignores @layer blocks. */
object LegacyArticleCss {
    fun flattenLayers(css: String): String {
        val out = StringBuilder(css.length)
        val braces = ArrayList<Boolean>()
        var i = 0
        while (i < css.length) {
            if (css.startsWith("/*", i)) {
                val end = css.indexOf("*/", i + 2).let { if (it < 0) css.length else it + 2 }
                out.append(css, i, end); i = end; continue
            }
            val ch = css[i]
            if (ch == '\'' || ch == '"') {
                val start = i++
                while (i < css.length) {
                    if (css[i] == '\\') { i = (i + 2).coerceAtMost(css.length); continue }
                    if (css[i++] == ch) break
                }
                out.append(css, start, i); continue
            }
            if (css.regionMatches(i, "@layer", 0, 6, ignoreCase = true) &&
                (i + 6 == css.length || css[i + 6].isWhitespace() || css[i + 6] == '{' || css[i + 6] == ';')) {
                var end = i + 6
                while (end < css.length && css[end] != '{' && css[end] != ';') end++
                if (end < css.length) {
                    if (css[end] == '{') braces.add(true)
                    i = end + 1; continue
                }
            }
            if (ch == '{') braces.add(false)
            if (ch == '}' && braces.isNotEmpty() && braces.removeAt(braces.lastIndex)) { i++; continue }
            out.append(ch); i++
        }
        return out.toString()
    }
}
