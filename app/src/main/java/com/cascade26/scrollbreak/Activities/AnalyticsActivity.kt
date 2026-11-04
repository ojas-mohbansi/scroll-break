package com.cascade26.scrollbreak.Activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cascade26.scrollbreak.Utils.SessionTelemetryManager

class AnalyticsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var metrics by mutableStateOf<SessionTelemetryManager.Metrics?>(null)
        SessionTelemetryManager.get(this).loadMetrics { loaded ->
            metrics = loaded
        }
        setContent {
            AnalyticsScreen(
                telemetryMetrics = metrics,
                onBack = { finish() }
            )
        }
    }
}
