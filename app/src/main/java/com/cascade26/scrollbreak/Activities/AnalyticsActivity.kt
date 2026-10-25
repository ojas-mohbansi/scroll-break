package com.cascade26.scrollbreak.Activities

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cascade26.scrollbreak.R
import com.cascade26.scrollbreak.Utils.SessionTelemetryManager
import java.util.concurrent.TimeUnit

class AnalyticsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        findViewById<TextView>(R.id.analyticsBack).setOnClickListener { finish() }
        SessionTelemetryManager.get(this).loadMetrics { metrics ->
            val minutes = TimeUnit.MILLISECONDS.toMinutes(metrics.timeSavedMs)
            findViewById<TextView>(R.id.totalTimeSaved).text = getString(R.string.analytics_time_saved_value, minutes)
            findViewById<TextView>(R.id.interventionSuccessRate).text =
                getString(R.string.analytics_percent_value, metrics.successPercent)
            findViewById<TextView>(R.id.goalAdherence).text =
                getString(R.string.analytics_percent_value, metrics.goalAdherencePercent)
            findViewById<TextView>(R.id.analyticsSessions).text =
                getString(R.string.analytics_sessions_value, metrics.interventionCount)
        }
    }
}
