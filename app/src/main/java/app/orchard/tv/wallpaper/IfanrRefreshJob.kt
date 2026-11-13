package app.orchard.tv.wallpaper

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.UserManager
import kotlinx.coroutines.*

/** Only the selected ifanr screensaver owns this persisted, network-constrained job. */
object IfanrRefresh {
    const val JOB_ID = 10420
    private var preferences: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences("wallpaper", Context.MODE_PRIVATE)
            .getString("screensaver", null) == "ifanr"

    fun initialize(context: Context) {
        if (Build.VERSION.SDK_INT >= 24 &&
            !(context.getSystemService(Context.USER_SERVICE) as UserManager).isUserUnlocked) return
        val app = context.applicationContext
        if (preferences == null) {
            preferences = app.getSharedPreferences("wallpaper", Context.MODE_PRIVATE)
            listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "screensaver" || key == null) reconcile(app)
            }
            preferences!!.registerOnSharedPreferenceChangeListener(listener!!)
        }
        reconcile(app)
    }

    fun reconcile(context: Context) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        if (!enabled(context)) { scheduler.cancel(JOB_ID); return }
        val pending = scheduler.allPendingJobs.firstOrNull { it.id == JOB_ID }
        if (pending != null && pending.intervalMillis == IfanrArticles.REFRESH_MS && pending.isPersisted) return
        scheduler.schedule(JobInfo.Builder(JOB_ID, ComponentName(context, IfanrRefreshJob::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(IfanrArticles.REFRESH_MS)
            .setPersisted(true)
            .build())
    }
}

class IfanrRefreshJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var task: Job? = null
    override fun onStartJob(params: JobParameters): Boolean {
        if (!IfanrRefresh.enabled(this)) { IfanrRefresh.reconcile(this); return false }
        task = scope.launch {
            try {
                IfanrArticles.refresh(applicationContext)
                ensureActive()
                getSharedPreferences("ifanr_refresh", Context.MODE_PRIVATE).edit()
                    .putLong("last_job_at", System.currentTimeMillis())
                    .putString("last_result", IfanrArticles.status).apply()
            } finally {
                if (isActive) jobFinished(params, false)
            }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { task?.cancel(); return false }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
