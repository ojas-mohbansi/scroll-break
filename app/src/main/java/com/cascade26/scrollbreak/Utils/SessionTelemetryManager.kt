package com.cascade26.scrollbreak.Utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import com.cascade26.scrollbreak.db.ScrollBreakRoomDatabase
import com.cascade26.scrollbreak.db.entities.SessionLogEntity
import java.util.concurrent.Executors
import kotlin.math.max

/** Serializes service/UI telemetry work away from accessibility callbacks and the main thread. */
class SessionTelemetryManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = ScrollBreakRoomDatabase.getInstance(appContext)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @JvmOverloads
    fun startSession(packageName: String, appName: String, startedAt: Long = System.currentTimeMillis()) {
        executor.execute {
            val dao = database.sessionLogDao()
            dao.closeAbandonedSessions(startedAt)
            if (dao.getActive(packageName) != null) return@execute
            val goalMinutes = PreferenceManager.getDefaultSharedPreferences(appContext)
                .getInt(PREF_SESSION_GOAL_MINUTES, DEFAULT_SESSION_GOAL_MINUTES)
                .coerceIn(5, 60)
            dao.insert(SessionLogEntity(packageName, appName, startedAt, goalMinutes * 60_000L))
        }
    }

    @JvmOverloads
    fun markIntervention(packageName: String, at: Long = System.currentTimeMillis()) {
        executor.execute {
            database.sessionLogDao().getActive(packageName)?.let { database.sessionLogDao().markIntervention(it.id, at) }
        }
    }

    @JvmOverloads
    fun finishWithoutIntervention(packageName: String, endedAt: Long = System.currentTimeMillis()) {
        executor.execute {
            val dao = database.sessionLogDao()
            val active = dao.getActive(packageName) ?: return@execute
            if (!active.interventionTriggered) {
                dao.finishWithoutIntervention(active.id, endedAt, max(0L, endedAt - active.startedAt))
            }
        }
    }

    fun finishIntervention(
        packageName: String,
        declaredIntent: String?,
        success: Boolean,
        endedAt: Long = System.currentTimeMillis()
    ) {
        executor.execute {
            val dao = database.sessionLogDao()
            val active = dao.getActive(packageName) ?: return@execute
            val duration = max(0L, endedAt - active.startedAt)
            val intent = declaredIntent?.trim()?.takeIf { it.isNotEmpty() }
            val savedMs = if (success) max(0L, active.sessionGoalMs - duration) else 0L
            dao.finishIntervention(active.id, intent, endedAt, duration, success, savedMs)
        }
    }

    fun loadMetrics(callback: (Metrics) -> Unit) {
        executor.execute {
            val dao = database.sessionLogDao()
            val total = dao.getResolvedInterventionCount()
            val successful = dao.getSuccessfulInterventionCount()
            val declared = dao.getIntentDeclaredInterventionCount()
            val adherent = dao.getIntentDeclaredSuccessfulCount()
            val metrics = Metrics(
                timeSavedMs = dao.getTotalSavedTimeMs(),
                successPercent = percentage(successful, total),
                goalAdherencePercent = percentage(adherent, declared),
                interventionCount = total
            )
            mainHandler.post { callback(metrics) }
        }
    }

    data class Metrics(
        val timeSavedMs: Long,
        val successPercent: Int,
        val goalAdherencePercent: Int,
        val interventionCount: Int
    )

    companion object {
        const val PREF_CLASSIFIER_ENABLED = "pref_scroll_classifier_enabled"
        const val PREF_SESSION_GOAL_MINUTES = "pref_scroll_session_goal_minutes"
        const val DEFAULT_SESSION_GOAL_MINUTES = 15

        @Volatile private var instance: SessionTelemetryManager? = null

        @JvmStatic
        fun get(context: Context): SessionTelemetryManager = instance ?: synchronized(this) {
            instance ?: SessionTelemetryManager(context).also { instance = it }
        }

        private fun percentage(numerator: Int, denominator: Int): Int =
            if (denominator == 0) 0 else ((numerator * 100f) / denominator).toInt()
    }
}
