package app.orchard.tv.settings

import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ConfigBackupTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    @Test fun roundTripRestoresTypedValuesAndSavesRollback() {
        val prefs = context.getSharedPreferences("settings", 0)
        prefs.edit().putInt("home_columns", 4).putBoolean("large_clock", true)
            .putStringSet("hidden_sources", setOf("hdmi:1")).putString("pin_hash", "local-secret").commit()
        context.getSharedPreferences("weather", 0).edit().putFloat("lat", 23.1f).commit()
        val backup = ConfigBackup.export(context).toString()
        prefs.edit().putInt("home_columns", 6).putString("pin_hash", "new-secret").commit()
        ConfigBackup.restore(context, backup)
        assertEquals(4, prefs.getInt("home_columns", 0))
        assertTrue(prefs.getBoolean("large_clock", false))
        assertEquals(setOf("hdmi:1"), prefs.getStringSet("hidden_sources", null))
        assertEquals("new-secret", prefs.getString("pin_hash", null))
        assertFalse(backup.contains("local-secret"))
        val rollback = ConfigBackup.validate(ConfigBackup.previous(context).readText())
        assertEquals(6, rollback.getJSONObject("preferences").getJSONObject("settings").getInt("home_columns"))
    }
    @Test fun wrongTypeDoesNotChangeAnything() {
        val prefs = context.getSharedPreferences("settings", 0)
        prefs.edit().putInt("home_columns", 4).commit()
        val doc = ConfigBackup.export(context)
        doc.getJSONObject("preferences").getJSONObject("settings").put("home_columns", "six")
        assertThrows(Exception::class.java) { ConfigBackup.restore(context, doc.toString()) }
        assertEquals(4, prefs.getInt("home_columns", 0))
        assertFalse(ConfigBackup.previous(context).exists())
    }
    @Test fun refusesUnknownKeysAndVersions() {
        val doc = ConfigBackup.export(context)
        doc.getJSONObject("preferences").getJSONObject("settings").put("pin_hash", "injected")
        assertThrows(Exception::class.java) { ConfigBackup.validate(doc.toString()) }
        doc.getJSONObject("preferences").getJSONObject("settings").remove("pin_hash")
        doc.put("version", 2)
        assertThrows(Exception::class.java) { ConfigBackup.validate(doc.toString()) }
    }
    @Test fun removesAbsentPortableKeysButKeepsLocalKeys() {
        val empty = ConfigBackup.export(context).toString()
        val prefs = context.getSharedPreferences("settings", 0)
        prefs.edit().putString("effects", "full").putBoolean("setup_offered", true).commit()
        ConfigBackup.restore(context, empty)
        assertFalse(prefs.contains("effects"))
        assertTrue(prefs.getBoolean("setup_offered", false))
    }
    @Test fun rejectsOutOfRangeAndOversizeFiles() {
        val doc = ConfigBackup.export(context)
        doc.getJSONObject("preferences").getJSONObject("settings").put("home_columns", -1)
        assertThrows(Exception::class.java) { ConfigBackup.validate(doc.toString()) }
        assertThrows(Exception::class.java) { ConfigBackup.validate(" ".repeat(ConfigBackup.MAX_BYTES + 1)) }
    }
}
