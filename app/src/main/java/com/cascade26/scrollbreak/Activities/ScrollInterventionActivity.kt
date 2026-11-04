package com.cascade26.scrollbreak.Activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.cascade26.scrollbreak.R
import com.cascade26.scrollbreak.Services.AppUsageAccessibilityService
import com.cascade26.scrollbreak.Utils.SessionTelemetryManager

class ScrollInterventionActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var resolved = false
    private lateinit var targetPackage: String
    private lateinit var intentInput: EditText

    private val timeout = Runnable { resolve(success = false, returnToHome = true) }
    private val focusLostResolution = Runnable {
        if (!resolved && !isFinishing) resolve(success = true, returnToHome = false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_intervention)

        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        val appName = intent.getStringExtra(EXTRA_APP_NAME).orEmpty().ifBlank { getString(R.string.app_name) }
        findViewById<TextView>(R.id.interventionAppName).text = appName
        val reason = when (intent.getStringExtra(EXTRA_TRIGGER_REASON)) {
            REASON_SESSION_OVERRUN -> R.string.session_trigger_overrun
            REASON_REPEATED_EXIT -> R.string.session_trigger_exit
            else -> R.string.session_trigger_scroll
        }
        findViewById<TextView>(R.id.interventionReason).setText(reason)
        val goalMinutes = intent.getIntExtra(EXTRA_SESSION_GOAL_MINUTES, 0)
        findViewById<TextView>(R.id.interventionSessionGoal).apply {
            text = if (goalMinutes > 0) getString(R.string.session_goal_value, goalMinutes) else ""
            visibility = if (goalMinutes > 0) android.view.View.VISIBLE else android.view.View.GONE
        }
        val purpose = intent.getStringExtra(EXTRA_USER_PURPOSE)
        findViewById<TextView>(R.id.interventionPurpose).apply {
            text = purpose?.let { getString(R.string.intervention_purpose_value, it) }.orEmpty()
            visibility = if (purpose.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
        }
        intentInput = findViewById(R.id.declaredIntentInput)

        findViewById<Button>(R.id.interventionDone).setOnClickListener {
            resolve(success = true, returnToHome = true)
        }
        findViewById<Button>(R.id.interventionContinue).setOnClickListener {
            resolve(success = false, returnToHome = true)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = resolve(success = false, returnToHome = true)
        })

        handler.postDelayed(timeout, OUTCOME_WINDOW_MS)
    }

    private fun resolve(success: Boolean, returnToHome: Boolean) {
        if (resolved) return
        resolved = true
        handler.removeCallbacks(timeout)
        val declaredIntent = intentInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val durationMs = intent.getLongExtra(EXTRA_SESSION_DURATION_MS, -1L).takeIf { it >= 0L }
        SessionTelemetryManager.get(this).finishIntervention(
            targetPackage, declaredIntent, success, null, durationMs
        )
        sendBroadcast(Intent(ACTION_OUTCOME_RESOLVED).setPackage(packageName)
            .putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
            .putExtra(EXTRA_OUTCOME_SUCCESS, success))
        if (returnToHome) {
            sendBroadcast(Intent(AppUsageAccessibilityService.ACTION_PERFORM_GLOBAL_HOME_FROM_OVERLAY)
                .setPackage(packageName))
        }
        finish()
    }

    override fun onStart() {
        handler.removeCallbacks(focusLostResolution)
        super.onStart()
    }

    override fun onStop() {
        if (!isChangingConfigurations && !isFinishing && !resolved) {
            // A system Home/app-switch gesture is also an exit from the target app.
            // Wait out brief activity handoffs before treating the lost focus as
            // the user's decision to leave; Android can stop/restart the screen
            // while the intervention Activity is being brought to the foreground.
            handler.postDelayed(focusLostResolution, FOCUS_LOSS_CONFIRMATION_MS)
        }
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "scroll_classifier_target_package"
        const val EXTRA_OUTCOME_SUCCESS = "scroll_classifier_outcome_success"
        const val EXTRA_APP_NAME = "scroll_classifier_app_name"
        const val EXTRA_USER_PURPOSE = "scroll_classifier_user_purpose"
        const val EXTRA_SESSION_DURATION_MS = "scroll_classifier_session_duration_ms"
        const val EXTRA_SESSION_GOAL_MINUTES = "scroll_classifier_session_goal_minutes"
        const val EXTRA_TRIGGER_REASON = "scroll_classifier_trigger_reason"
        const val REASON_RAPID_SCROLL = "rapid_scroll"
        const val REASON_SESSION_OVERRUN = "session_overrun"
        const val REASON_REPEATED_EXIT = "repeated_exit"
        const val ACTION_OUTCOME_RESOLVED = "com.cascade26.scrollbreak.action.SCROLL_OUTCOME_RESOLVED"
        const val OUTCOME_WINDOW_MS = 30_000L
        const val FOCUS_LOSS_CONFIRMATION_MS = 1_500L
    }
}
