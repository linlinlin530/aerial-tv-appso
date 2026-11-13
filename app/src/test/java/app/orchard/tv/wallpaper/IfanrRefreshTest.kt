package app.orchard.tv.wallpaper

import android.app.Application
import android.app.job.JobScheduler
import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23], application = Application::class)
class IfanrRefreshTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val scheduler get() = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    private fun select(mode: String?) {
        context.getSharedPreferences("wallpaper", Context.MODE_PRIVATE).edit().putString("screensaver", mode).commit()
        IfanrRefresh.reconcile(context)
    }
    @Before fun reset() { scheduler.cancelAll(); select(null) }
    @Test fun selectedIfanrSchedulesPersistedNetworkJobEveryTwoHours() {
        select("ifanr")
        val job = scheduler.allPendingJobs.single()
        assertEquals(7_200_000L, job.intervalMillis)
        assertTrue(job.isPersisted)
        assertEquals(android.app.job.JobInfo.NETWORK_TYPE_ANY, job.networkType)
        assertEquals(IfanrRefreshJob::class.java.name, job.service.className)
    }
    @Test fun otherModesCancelOnlyIfanrJobAndCanReenable() {
        select("ifanr")
        val unrelated = android.app.job.JobInfo.Builder(99, android.content.ComponentName(context, IfanrRefreshJob::class.java)).setMinimumLatency(1000).build()
        scheduler.schedule(unrelated)
        for (mode in listOf("photos", "aerial", "system", null)) {
            select(mode)
            assertFalse(scheduler.allPendingJobs.any { it.id == IfanrRefresh.JOB_ID })
            assertTrue(scheduler.allPendingJobs.any { it.id == 99 })
            select("ifanr")
            assertEquals(1, scheduler.allPendingJobs.count { it.id == IfanrRefresh.JOB_ID })
        }
    }
    @Test fun reconcileDoesNotReplaceExistingSchedule() {
        select("ifanr")
        val before = scheduler.allPendingJobs.single()
        IfanrRefresh.reconcile(context)
        assertSame(before, scheduler.allPendingJobs.single())
    }
}
