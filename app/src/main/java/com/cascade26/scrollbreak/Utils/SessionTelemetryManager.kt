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

    fun startSession(packageName: String, appName: String, startedAt: Long = System.currentTimeMillis()) =
        startSession(packageName, appName, null, null, MODE_STANDARD, null, null, startedAt)

    fun startSession(
        packageName: String,
        appName: String,
        purpose: String?,
        goalMinutes: Int?,
        sessionMode: String?,
        evaluationGroup: String?,
        evaluationSessionId: String?,
        startedAt: Long = System.currentTimeMillis()
    ) {
        executor.execute {
            val dao = database.sessionLogDao()
            dao.closeAbandonedSessions(startedAt)
            if (dao.getActive(packageName) != null) return@execute
            val configuredGoalMinutes = goalMinutes ?: PreferenceManager.getDefaultSharedPreferences(appContext)
                .getInt(PREF_SESSION_GOAL_MINUTES, DEFAULT_SESSION_GOAL_MINUTES)
            val session = SessionLogEntity(
                packageName, appName, startedAt, configuredGoalMinutes.coerceIn(5, 60) * 60_000L
            ).apply {
                userPurpose = purpose?.trim()?.takeIf { it.isNotEmpty() }
                this.sessionMode = sessionMode?.takeIf { it == MODE_LONG_FORM } ?: MODE_STANDARD
                this.evaluationGroup = evaluationGroup?.takeIf {
                    it == EVALUATION_INTERVENTION || it == EVALUATION_CONTROL
                }
                this.evaluationSessionId = evaluationSessionId?.takeIf { it.isNotBlank() }
            }
            dao.insert(session)
        }
    }

    @JvmOverloads
    fun markIntervention(packageName: String, at: Long = System.currentTimeMillis()) {
        executor.execute {
            database.sessionLogDao().getActive(packageName)?.let { database.sessionLogDao().markIntervention(it.id, at) }
        }
    }

    fun markInterventionShown(packageName: String) {
        executor.execute {
            database.sessionLogDao().getActive(packageName)?.let {
                database.sessionLogDao().markInterventionShown(it.id)
            }
        }
    }

    fun recordLongFormEvidence(packageName: String, dwellMs: Long) {
        executor.execute { database.sessionLogDao().recordLongFormEvidence(packageName, max(0L, dwellMs)) }
    }

    fun markLongFormSurface(packageName: String) {
        executor.execute { database.sessionLogDao().markLongFormSurface(packageName) }
    }

    fun recordSessionMode(packageName: String, mode: String) {
        executor.execute { database.sessionLogDao().recordSessionMode(packageName, mode) }
    }

    fun recordEvaluationGroup(packageName: String, group: String?) {
        if (group == null) return
        executor.execute { database.sessionLogDao().recordEvaluationGroup(packageName, group) }
    }

    @JvmOverloads
    fun markOverrun(packageName: String, overrunMs: Long, at: Long = System.currentTimeMillis()) {
        executor.execute {
            database.sessionLogDao().getActive(packageName)?.let { active ->
                database.sessionLogDao().markOverrun(active.id, max(0L, overrunMs))
                database.sessionLogDao().markIntervention(active.id, at)
            }
        }
    }

    fun recordExitAttempt(packageName: String, callback: (Int) -> Unit) {
        executor.execute {
            val dao = database.sessionLogDao()
            val active = dao.getActive(packageName) ?: return@execute
            dao.incrementExitAttempts(active.id)
            val updated = dao.getActive(packageName)
            mainHandler.post { callback(updated?.exitAttempts ?: 0) }
        }
    }

    @JvmOverloads
    fun finishWithoutIntervention(
        packageName: String,
        endedAt: Long = System.currentTimeMillis(),
        durationMs: Long? = null
    ) {
        executor.execute {
            val dao = database.sessionLogDao()
            val active = dao.getActive(packageName) ?: return@execute
            // If the user leaves while an intervention is pending, close the row
            // as an unresolved/unsuccessful intervention instead of leaving it active.
            dao.finishWithoutIntervention(
                active.id, endedAt, max(0L, durationMs ?: (endedAt - active.startedAt))
            )
        }
    }

    fun finishIntervention(
        packageName: String,
        declaredIntent: String?,
        success: Boolean,
        purposeCompleted: Boolean? = null,
        durationMs: Long? = null,
        endedAt: Long = System.currentTimeMillis()
    ) {
        executor.execute {
            val dao = database.sessionLogDao()
            val active = dao.getActive(packageName) ?: return@execute
            dao.markInterventionShown(active.id)
            val duration = max(0L, durationMs ?: (endedAt - active.startedAt))
            val intent = declaredIntent?.trim()?.takeIf { it.isNotEmpty() }
            val savedMs = if (success) max(0L, active.sessionGoalMs - duration) else 0L
            dao.finishIntervention(active.id, intent, endedAt, duration, success, savedMs, purposeCompleted)
        }
    }

    fun finishEvaluationOutcome(evaluationSessionId: String, purposeCompleted: Boolean?) {
        executor.execute {
            val dao = database.sessionLogDao()
            val latest = dao.getUnreportedEvaluationOutcome(evaluationSessionId) ?: return@execute
            dao.recordResolvedPurposeOutcome(latest.id, purposeCompleted)
        }
    }

    fun loadMetrics(callback: (Metrics) -> Unit) {
        executor.execute {
            val dao = database.sessionLogDao()
            val total = dao.getResolvedInterventionCount()
            val successful = dao.getSuccessfulInterventionCount()
            val declared = dao.getIntentDeclaredInterventionCount()
            val adherent = dao.getIntentDeclaredSuccessfulCount()
            val purposeOutcomeCount = dao.getPurposeOutcomeCount()
            val purposeCompleted = dao.getPurposeCompletedCount()
            val longFormCount = dao.getLongFormSessionCount()
            val longFormSurfaceCount = dao.getLongFormSurfaceSessionCount()
            val intervenedPurposeCount = dao.getIntervenedPurposeOutcomeCount()
            val intervenedPurposeCompleted = dao.getIntervenedPurposeCompletedCount()
            val baselinePurposeCount = dao.getBaselinePurposeOutcomeCount()
            val baselinePurposeCompleted = dao.getBaselinePurposeCompletedCount()
            val randomizedInterventionOutcomeCount = dao.getAssignedInterventionPurposeOutcomeCount()
            val randomizedInterventionCompleted = dao.getAssignedInterventionPurposeCompletedCount()
            val randomizedControlOutcomeCount = dao.getControlPurposeOutcomeCount()
            val randomizedControlCompleted = dao.getControlPurposeCompletedCount()
            val randomizedInterventionAssignedCount = dao.getAssignedInterventionSessionCount()
            val randomizedInterventionCompletedSessionCount = dao.getAssignedInterventionCompletedSessionCount()
            val randomizedControlAssignedCount = dao.getControlSessionCount()
            val randomizedControlCompletedSessionCount = dao.getControlCompletedSessionCount()
            val metrics = Metrics(
                timeSavedMs = dao.getTotalSavedTimeMs(),
                successPercent = percentage(successful, total),
                goalAdherencePercent = percentage(adherent, declared),
                interventionCount = total,
                overrunCount = dao.getOverrunSessionCount(),
                totalOverrunMs = dao.getTotalOverrunMs(),
                purposeCompletionPercent = percentage(purposeCompleted, purposeOutcomeCount),
                longFormSessionCount = longFormCount,
                longFormSurfaceOutcomeCount = longFormSurfaceCount,
                longFormSurfacePercent = percentage(longFormSurfaceCount, longFormCount),
                longFormPurposeCompletionPercent = percentage(
                    dao.getLongFormPurposeCompletedCount(), dao.getLongFormPurposeOutcomeCount()
                ),
                longFormPurposeOutcomeCount = dao.getLongFormPurposeOutcomeCount(),
                intervenedPurposeCompletionPercent = percentage(intervenedPurposeCompleted, intervenedPurposeCount),
                intervenedPurposeOutcomeCount = intervenedPurposeCount,
                baselinePurposeCompletionPercent = percentage(baselinePurposeCompleted, baselinePurposeCount),
                baselinePurposeOutcomeCount = baselinePurposeCount,
                randomizedInterventionCompletionPercent = percentage(
                    randomizedInterventionCompleted, randomizedInterventionOutcomeCount
                ),
                randomizedInterventionOutcomeCount = randomizedInterventionOutcomeCount,
                randomizedInterventionAssignedCount = randomizedInterventionAssignedCount,
                randomizedInterventionCompletedSessionCount = randomizedInterventionCompletedSessionCount,
                randomizedControlCompletionPercent = percentage(randomizedControlCompleted, randomizedControlOutcomeCount),
                randomizedControlOutcomeCount = randomizedControlOutcomeCount,
                randomizedControlAssignedCount = randomizedControlAssignedCount,
                randomizedControlCompletedSessionCount = randomizedControlCompletedSessionCount
            )
            mainHandler.post { callback(metrics) }
        }
    }

    data class Metrics(
        val timeSavedMs: Long,
        val successPercent: Int,
        val goalAdherencePercent: Int,
        val interventionCount: Int,
        val overrunCount: Int,
        val totalOverrunMs: Long,
        val purposeCompletionPercent: Int,
        val longFormSessionCount: Int,
        val longFormSurfaceOutcomeCount: Int,
        val longFormSurfacePercent: Int,
        val longFormPurposeCompletionPercent: Int,
        val longFormPurposeOutcomeCount: Int,
        val intervenedPurposeCompletionPercent: Int,
        val intervenedPurposeOutcomeCount: Int,
        val baselinePurposeCompletionPercent: Int,
        val baselinePurposeOutcomeCount: Int,
        val randomizedInterventionCompletionPercent: Int,
        val randomizedInterventionOutcomeCount: Int,
        val randomizedInterventionAssignedCount: Int,
        val randomizedInterventionCompletedSessionCount: Int,
        val randomizedControlCompletionPercent: Int,
        val randomizedControlOutcomeCount: Int,
        val randomizedControlAssignedCount: Int,
        val randomizedControlCompletedSessionCount: Int
    )

    companion object {
        const val PREF_CLASSIFIER_ENABLED = "pref_scroll_classifier_enabled"
        const val PREF_SESSION_GOAL_MINUTES = "pref_scroll_session_goal_minutes"
        const val PREF_ACTIVE_SESSION_PACKAGE = "pref_scroll_active_session_package"
        const val PREF_ACTIVE_SESSION_STARTED_AT = "pref_scroll_active_session_started_at"
        const val PREF_ACTIVE_SESSION_GOAL_MINUTES = "pref_scroll_active_session_goal_minutes"
        const val PREF_ACTIVE_SESSION_OVERRUN_MS = "pref_scroll_active_session_overrun_ms"
        const val PREF_ACTIVE_SESSION_EXIT_ATTEMPTS = "pref_scroll_active_session_exit_attempts"
        const val PREF_ACTIVE_SESSION_MODE = "pref_scroll_active_session_mode"
        const val PREF_ACTIVE_SESSION_EVALUATION_GROUP = "pref_scroll_active_session_evaluation_group"
        const val PREF_ACTIVE_SESSION_EVALUATION_ENABLED = "pref_scroll_active_session_evaluation_enabled"
        const val PREF_ACTIVE_SESSION_EVALUATION_SESSION_ID = "pref_scroll_active_session_evaluation_session_id"
        const val DEFAULT_SESSION_GOAL_MINUTES = 15
        const val MODE_STANDARD = "standard"
        const val MODE_LONG_FORM = "long_form"
        const val EVALUATION_INTERVENTION = "intervention"
        const val EVALUATION_CONTROL = "control"

        @Volatile private var instance: SessionTelemetryManager? = null

        @JvmStatic
        fun get(context: Context): SessionTelemetryManager = instance ?: synchronized(this) {
            instance ?: SessionTelemetryManager(context).also { instance = it }
        }

        private fun percentage(numerator: Int, denominator: Int): Int =
            if (denominator == 0) 0 else ((numerator * 100f) / denominator).toInt()
    }
}
