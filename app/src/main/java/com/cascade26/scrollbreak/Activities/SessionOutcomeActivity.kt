package com.cascade26.scrollbreak.Activities

import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cascade26.scrollbreak.R
import com.cascade26.scrollbreak.Utils.SessionTelemetryManager

/** Optional purpose outcome report at the end of a randomized evaluation session. */
class SessionOutcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_outcome)
        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val evaluationSessionId = intent.getStringExtra(EXTRA_EVALUATION_SESSION_ID).orEmpty()
        if (targetPackage.isBlank() || evaluationSessionId.isBlank()) {
            finish()
            return
        }
        findViewById<TextView>(R.id.sessionOutcomePurpose).text =
            intent.getStringExtra(EXTRA_PURPOSE).orEmpty()
        findViewById<Button>(R.id.sessionOutcomeSave).setOnClickListener {
            val result = when (findViewById<RadioGroup>(R.id.sessionOutcomeGroup).checkedRadioButtonId) {
                R.id.sessionOutcomeYes -> true
                R.id.sessionOutcomeNo -> false
                else -> null
            }
            if (result == null) {
                Toast.makeText(this, R.string.session_outcome_choose, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SessionTelemetryManager.get(this).finishEvaluationOutcome(evaluationSessionId, result)
            finish()
        }
        findViewById<Button>(R.id.sessionOutcomeSkip).setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_PACKAGE = "session_outcome_package"
        const val EXTRA_PURPOSE = "session_outcome_purpose"
        const val EXTRA_EVALUATION_SESSION_ID = "session_outcome_evaluation_session_id"
    }
}
