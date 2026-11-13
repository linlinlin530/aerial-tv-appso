package app.orchard.tv.settings

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Portable preferences only. Device permissions, pairing tokens and parental controls stay local. */
object ConfigBackup {
    const val MAX_BYTES = 512 * 1024
    private val schema = mapOf(
        "settings" to buildMap {
            listOf("one_row_home", "peek_next_row", "clock_right", "large_clock", "home_locked", "home_plate", "show_all_names", "wallpaper_always_moves", "show_weather", "screensaver_clock", "back_starts_screensaver", "cec_switch", "ui_sounds").forEach { put(it, "boolean") }
            listOf("home_columns", "photo_minutes", "screensaver_minutes").forEach { put(it, "int") }
            listOf("focus_style", "clock_hours", "ui_sounds_kind", "aerial_quality", "effects", "boot_source", "boot_app", "app_names", "source_looks").forEach { put(it, "string") }
            listOf("pinned_sources", "hidden_sources").forEach { put(it, "set") }
        },
        "layout" to mapOf("order" to "string", "hidden" to "set", "folders" to "string", "initialized" to "boolean"),
        "wallpaper" to mapOf("selected" to "string", "screensaver" to "string"),
        "icon_library" to mapOf("use_library" to "set", "use_own" to "set"),
        "weather" to mapOf("lat" to "float", "lon" to "float", "place" to "string", "chosen" to "boolean", "checked_against_cities" to "boolean")
    )
    private fun type(group: String, key: String): String? = schema[group]?.get(key)
        ?: if (group == "icon_library" && key.startsWith("choice:") && key.length <= 256) "string" else null

    @Synchronized fun export(context: Context): JSONObject {
        val groups = JSONObject()
        schema.keys.forEach { group ->
            val entries = JSONObject()
            context.getSharedPreferences(group, 0).all.toSortedMap().forEach { (key, value) ->
                if (type(group, key) != null && value != null) entries.put(key, if (value is Set<*>) JSONArray(value.filterIsInstance<String>().sorted()) else value)
            }
            groups.put(group, entries)
        }
        return JSONObject().put("format", "aerial-config").put("version", 1)
            .put("createdAt", System.currentTimeMillis()).put("preferences", groups)
    }

    /** Validate everything before writing anything; reject unknown keys and mismatched types. */
    fun validate(text: String): JSONObject {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "File too large" }
        val root = JSONObject(text)
        require(root.optString("format") == "aerial-config" && root.get("version") == 1) { "Unsupported backup format" }
        val groups = root.getJSONObject("preferences")
        require(groups.keys().asSequence().toSet() == schema.keys) { "Missing or unknown groups" }
        groups.keys().forEach { group ->
            val entries = groups.getJSONObject(group)
            require(entries.length() <= 2000)
            entries.keys().forEach { key ->
                val value = entries.get(key)
                val valid = when (type(group, key)) {
                    "boolean" -> value is Boolean
                    "string" -> value is String && value.length <= 100000
                    "int" -> value is Int
                    "float" -> value is Number && value.toDouble().isFinite()
                    "set" -> value is JSONArray && value.length() <= 2000 && (0 until value.length()).all { value.get(it) is String && value.getString(it).length <= 512 }
                    else -> false
                }
                require(valid) { "Invalid preference: $group/$key" }
                if (key == "home_columns") require(entries.getInt(key) in AerialSettings.HOME_COLUMNS)
                if (key == "screensaver_minutes") require(entries.getInt(key) in AerialSettings.screensaverChoices)
                if (key == "photo_minutes") require(entries.getInt(key) in AerialSettings.PHOTO_MINUTES)
                if (group == "weather" && key == "lat") require(entries.getDouble(key) in -90.0..90.0)
                if (group == "weather" && key == "lon") require(entries.getDouble(key) in -180.0..180.0)
                if (key in listOf("app_names", "source_looks")) JSONObject(entries.getString(key))
                if (group == "layout" && key == "folders") JSONArray(entries.getString(key))
            }
        }
        return root
    }

    private fun write(context: Context, root: JSONObject) {
        val groups = root.getJSONObject("preferences")
        schema.keys.forEach { group ->
            val prefs = context.getSharedPreferences(group, 0)
            val edit = prefs.edit()
            prefs.all.keys.filter { type(group, it) != null }.forEach(edit::remove)
            val entries = groups.getJSONObject(group)
            entries.keys().forEach { key ->
                when (type(group, key)) {
                    "boolean" -> edit.putBoolean(key, entries.getBoolean(key))
                    "string" -> edit.putString(key, entries.getString(key))
                    "int" -> edit.putInt(key, entries.getInt(key))
                    "float" -> edit.putFloat(key, entries.getDouble(key).toFloat())
                    "set" -> { val a = entries.getJSONArray(key); edit.putStringSet(key, (0 until a.length()).map { a.getString(it) }.toSet()) }
                }
            }
            check(edit.commit()) { "Could not save preferences" }
        }
    }

    fun previous(context: Context) = File(context.filesDir, "config-before-import.json")

    @Synchronized fun restore(context: Context, text: String) {
        val incoming = validate(text)
        val before = export(context)
        val temp = File(context.filesDir, "config-before-import.tmp")
        temp.writeText(before.toString(2))
        check(temp.renameTo(previous(context))) { "Could not save rollback copy" }
        try { write(context, incoming) } catch (error: Exception) {
            write(context, before)
            throw error
        }
    }
}
