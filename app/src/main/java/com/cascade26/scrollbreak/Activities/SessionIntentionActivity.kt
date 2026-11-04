package com.cascade26.scrollbreak.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.preference.PreferenceManager
import com.cascade26.scrollbreak.R
import com.cascade26.scrollbreak.Services.AppUsageAccessibilityService
import com.cascade26.scrollbreak.Utils.SessionTelemetryManager

/** Captures the user's purpose and time budget before the monitored app session starts. */
class SessionIntentionActivity : AppCompatActivity() {
    private lateinit var packageName: String
    private lateinit var appName: String
    private lateinit var purposeInput: EditText
    private lateinit var durationLabel: TextView
    private var goalMinutes = SessionTelemetryManager.DEFAULT_SESSION_GOAL_MINUTES
    private var resolved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_intention)

        packageName = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        appName = intent.getStringExtra(EXTRA_APP_NAME).orEmpty().ifBlank { getString(R.string.app_name) }
        if (packageName.isBlank()) {
            finish()
            return
        }

        findViewById<TextView>(R.id.sessionIntentAppName).text = appName
        purposeInput = findViewById(R.id.sessionPurposeInput)
        durationLabel = findViewById(R.id.sessionDurationLabel)
        val seekBar = findViewById<SeekBar>(R.id.sessionDurationSeekBar)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val configuredGoal = preferences
            .getInt(SessionTelemetryManager.PREF_SESSION_GOAL_MINUTES, SessionTelemetryManager.DEFAULT_SESSION_GOAL_MINUTES)
        goalMinutes = configuredGoal.coerceIn(5, 60)
        seekBar.max = 55
        seekBar.progress = goalMinutes - 5
        updateDurationLabel()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                goalMinutes = progress + 5
                updateDurationLabel()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        findViewById<Button>(R.id.sessionStartButton).setOnClickListener {
            val purpose = purposeInput.text?.toString()?.trim().orEmpty()
            if (purpose.isBlank()) {
                purposeInput.error = getString(R.string.session_purpose_required)
                purposeInput.requestFocus()
                return@setOnClickListener
            }
            val startedAt = System.currentTimeMillis()
            val selectedMode = when (findViewById<RadioGroup>(R.id.sessionModeGroup).checkedRadioButtonId) {
                R.id.sessionModeLongForm -> SessionTelemetryManager.MODE_LONG_FORM
                else -> SessionTelemetryManager.MODE_STANDARD
            }
            val evaluationGroup = if (findViewById<CheckBox>(R.id.sessionEvaluationOptIn).isChecked) {
                if (kotlin.random.Random.nextBoolean()) SessionTelemetryManager.EVALUATION_INTERVENTION
                else SessionTelemetryManager.EVALUATION_CONTROL
            } else null
            val evaluationSessionId = evaluationGroup?.let { java.util.UUID.randomUUID().toString() }
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            preferences.edit()
                .putString("session_purpose_$packageName", purpose)
                .putString("session_mode_$packageName", selectedMode)
                .putInt("session_goal_override_$packageName", goalMinutes)
                .putString(SessionTelemetryManager.PREF_ACTIVE_SESSION_PACKAGE, packageName)
                .putLong(SessionTelemetryManager.PREF_ACTIVE_SESSION_STARTED_AT, startedAt)
                .putInt(SessionTelemetryManager.PREF_ACTIVE_SESSION_GOAL_MINUTES, goalMinutes)
                .putLong(SessionTelemetryManager.PREF_ACTIVE_SESSION_OVERRUN_MS, 0L)
                .putInt(SessionTelemetryManager.PREF_ACTIVE_SESSION_EXIT_ATTEMPTS, 0)
                .putString(SessionTelemetryManager.PREF_ACTIVE_SESSION_MODE, selectedMode)
                .putString(SessionTelemetryManager.PREF_ACTIVE_SESSION_EVALUATION_GROUP, evaluationGroup)
                .putString(SessionTelemetryManager.PREF_ACTIVE_SESSION_EVALUATION_SESSION_ID, evaluationSessionId)
                .putBoolean(SessionTelemetryManager.PREF_ACTIVE_SESSION_EVALUATION_ENABLED, evaluationGroup != null)
                .apply()
            SessionTelemetryManager.get(this).startSession(
                packageName, appName, purpose, goalMinutes, selectedMode, evaluationGroup, evaluationSessionId, startedAt
            )
            resolved = true
            sendBroadcast(Intent(AppUsageAccessibilityService.ACTION_SESSION_INTENTION_RESOLVED)
                .setPackage(this.packageName)
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_GOAL_MINUTES, goalMinutes)
                .putExtra(EXTRA_STARTED_AT, startedAt))
            finish()
        }
        findViewById<Button>(R.id.sessionCancelButton).setOnClickListener {
            cancelSession()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = cancelSession()
        })
    }

    private fun updateDurationLabel() {
        durationLabel.text = getString(R.string.session_duration_value, goalMinutes)
    }

    private fun cancelSession() {
        if (resolved) return
        resolved = true
        Toast.makeText(this, R.string.session_not_started, Toast.LENGTH_SHORT).show()
        notifyCancelled(userDeclined = true)
        sendBroadcast(Intent(AppUsageAccessibilityService.ACTION_PERFORM_GLOBAL_HOME_FROM_OVERLAY)
            .setPackage(this.packageName))
        finish()
    }

    private fun notifyCancelled(userDeclined: Boolean) {
        sendBroadcast(Intent(AppUsageAccessibilityService.ACTION_SESSION_INTENTION_CANCELLED)
            .setPackage(this.packageName)
            .putExtra(EXTRA_PACKAGE, packageName)
            .putExtra(EXTRA_USER_DECLINED, userDeclined))
    }

    override fun onStop() {
        if (!isChangingConfigurations && !resolved) {
            resolved = true
            notifyCancelled(userDeclined = false)
        }
        super.onStop()
    }

    companion object {
        const val EXTRA_PACKAGE = "session_intention_package"
        const val EXTRA_APP_NAME = "session_intention_app_name"
        const val EXTRA_GOAL_MINUTES = "session_intention_goal_minutes"
        const val EXTRA_STARTED_AT = "session_intention_started_at"
        const val EXTRA_USER_DECLINED = "session_intention_user_declined"
    }
}
