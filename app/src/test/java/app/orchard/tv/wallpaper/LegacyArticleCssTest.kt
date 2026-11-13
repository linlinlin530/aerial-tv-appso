package app.orchard.tv.wallpaper
import org.junit.Assert.*
import org.junit.Test
class LegacyArticleCssTest {
    @Test fun unwrapsLayersAndPreservesMediaRules() {
        assertEquals("p{color:red}@media(min-width:1px){p{color:blue}}", LegacyArticleCss.flattenLayers("@layer base, responsive;@layer base{p{color:red}}@layer responsive{@media(min-width:1px){p{color:blue}}}"))
    }
    @Test fun preservesQuotedBracesCommentsAndEscapes() {
        val rule = "/* @layer fake{ } */p{content:\"} @layer test{ \\\"\";background:url(@layer.png)}"
        assertEquals(rule, LegacyArticleCss.flattenLayers("@layer real{" + rule + "}"))
    }
    @Test fun leavesOrdinaryCssUnchangedAndSupportsAnonymousLayers() {
        val css = "@font-face{font-family:x;src:url(a.woff)}p{color:red}"
        assertEquals(css, LegacyArticleCss.flattenLayers(css))
        assertEquals("p{color:red}", LegacyArticleCss.flattenLayers("@layer{@layer nested{p{color:red}}}"))
    }
}
