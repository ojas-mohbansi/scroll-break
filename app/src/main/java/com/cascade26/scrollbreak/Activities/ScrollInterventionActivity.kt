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
import com.cascade26.scrollbreak.R
import com.cascade26.scrollbreak.Services.AppUsageAccessibilityService
import com.cascade26.scrollbreak.Utils.SessionTelemetryManager

class ScrollInterventionActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var resolved = false
    private lateinit var targetPackage: String
    private lateinit var intentInput: EditText

    private val timeout = Runnable { resolve(success = false, returnToHome = true) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scroll_intervention)

        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        val appName = intent.getStringExtra(EXTRA_APP_NAME).orEmpty().ifBlank { getString(R.string.app_name) }
        findViewById<TextView>(R.id.interventionAppName).text = appName
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
        SessionTelemetryManager.get(this).finishIntervention(
            targetPackage, declaredIntent, success
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

    override fun onStop() {
        if (!isChangingConfigurations && !isFinishing && !resolved) {
            // A system Home/app-switch gesture is also an exit from the target app.
            // The user already left the monitored app. Record that exit without overriding
            // the destination they switched to with another forced Home navigation.
            resolve(success = true, returnToHome = false)
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
        const val ACTION_OUTCOME_RESOLVED = "com.cascade26.scrollbreak.action.SCROLL_OUTCOME_RESOLVED"
        const val OUTCOME_WINDOW_MS = 30_000L
    }
}
