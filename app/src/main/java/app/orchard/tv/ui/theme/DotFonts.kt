package app.orchard.tv.ui.theme

import android.app.Application
import android.graphics.Typeface

/** Shared static face for Compose, Canvas labels and native overlay text. */
object DotFonts {
    lateinit var native: Typeface
        private set

    fun initialize(application: Application) {
        native = Typeface.createFromAsset(application.assets, "fonts/aerial_dots.otf")
    }
}

class DotFontApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DotFonts.initialize(this)
        app.orchard.tv.wallpaper.IfanrRefresh.initialize(this)
    }
}
