package com.cascade26.scrollbreak.Activities

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.preference.PreferenceManager
import com.cascade26.scrollbreak.R
import com.cascade26.scrollbreak.Utils.AdultDomainListManager
import com.cascade26.scrollbreak.Utils.ChallengeLockManager
import com.cascade26.scrollbreak.Utils.SettingsLockManager
import com.cascade26.scrollbreak.db.ScrollBreakRoomDatabase
import com.cascade26.scrollbreak.db.dao.BlockedAppDao

class BlockerControlActivity : AppCompatActivity() {

    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var database: ScrollBreakRoomDatabase
    private lateinit var blockedAppDao: BlockedAppDao
    private lateinit var settingsLockMgr: SettingsLockManager
    private lateinit var challengeLockMgr: ChallengeLockManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var deviceAdminComponent: ComponentName

    private val isAuthenticatedState = mutableStateOf(false)
    private val settingsLockEnabledState = mutableStateOf(false)
    private val deviceAdminActiveState = mutableStateOf(false)

    private val deviceAdminLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        refreshDeviceAdminState()
        if (deviceAdminActiveState.value) {
            Toast.makeText(this, "Uninstall Protection enabled.", Toast.LENGTH_SHORT).show()
        } else if (result.resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Device Admin permission was not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    private val settingsLockChallengeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            settingsLockMgr.isLockEnabled = true
            settingsLockEnabledState.value = true
        } else {
            Toast.makeText(this, "Settings Lock remains off because the challenge was not completed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshDeviceAdminState() {
        if (::devicePolicyManager.isInitialized && ::deviceAdminComponent.isInitialized) {
            deviceAdminActiveState.value = devicePolicyManager.isAdminActive(deviceAdminComponent)
        }
    }

    private fun requestDeviceAdmin() {
        refreshDeviceAdminState()
        if (deviceAdminActiveState.value) return
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Android requires Device Admin to be active before it asks for confirmation when you uninstall Scroll Break."
            )
        }
        try {
            deviceAdminLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Android could not open the Device Admin screen.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDeviceAdminState()
    }

    private val challengeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            isAuthenticatedState.value = true
        } else {
            finish()
        }
    }

    private fun showAdultListDownloadDialogAndEnsure(onFinished: (Boolean) -> Unit) {
        val builder = AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_adult_list_progress, null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        val progressDialog = builder.create()
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        progressDialog.show()

        AdultDomainListManager.ensureLocalList(this,
            object : AdultDomainListManager.OnLocalListReadyListener {
                override fun onSuccess(mergedFileBytes: Long, sha256Hex: String?, deduped: Boolean) {
                    runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(this@BlockerControlActivity, "Local domain rules are ready", Toast.LENGTH_SHORT).show()
                        onFinished(true)
                    }
                }

                override fun onError(e: Exception) {
                    runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(this@BlockerControlActivity, "Unable to load local rules: ${e.message}", Toast.LENGTH_SHORT).show()
                        onFinished(false)
                    }
                }
            }, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        database = ScrollBreakRoomDatabase.getInstance(this)
        blockedAppDao = database.blockedAppDao()
        settingsLockMgr = SettingsLockManager(this)
        challengeLockMgr = ChallengeLockManager(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        deviceAdminComponent = ComponentName(this, com.cascade26.scrollbreak.Receivers.ScrollBreakDeviceAdminReceiver::class.java)
        settingsLockEnabledState.value = settingsLockMgr.isLockEnabled
        refreshDeviceAdminState()

        if (settingsLockMgr.isLockEnabled) {
            val lockType = sharedPrefs.getString(ChallengeLockManager.PREF_SETTINGS_LOCK_TYPE, "device") ?: "device"
            when (lockType) {
                "device", "custom" -> {
                    settingsLockMgr.authenticate(this, "Access Blocker & Lock Center", object : SettingsLockManager.AuthCallback {
                        override fun onSuccess() { isAuthenticatedState.value = true }
                        override fun onFailure(reason: String?) { finish() }
                    })
                }
                "oneday" -> {
                    if (challengeLockMgr.isSettingsOneDayLockActive) {
                        Toast.makeText(this, "Settings are locked under a 1-Day lockout.", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        isAuthenticatedState.value = true
                    }
                }
                else -> {
                    challengeLauncher.launch(
                        Intent(this, LockChallengeActivity::class.java).apply {
                            putExtra(LockChallengeActivity.EXTRA_LOCK_TYPE, lockType)
                            putExtra(LockChallengeActivity.EXTRA_IS_SETTINGS_LOCK, true)
                        }
                    )
                }
            }
        } else {
            isAuthenticatedState.value = true
        }

        setContent {
            val isAuthenticated by remember { isAuthenticatedState }

            if (isAuthenticated) {
                BlockerControlScreen(
                    onBackClick = { finish() },
                    sharedPrefs = sharedPrefs,
                    blockedAppDao = blockedAppDao,
                    settingsLockMgr = settingsLockMgr,
                    activityContext = this@BlockerControlActivity,
                    settingsLockEnabledValue = settingsLockEnabledState.value,
                    deviceAdminActive = deviceAdminActiveState.value,
                    onSettingsLockChanged = { enabled ->
                        settingsLockMgr.isLockEnabled = enabled
                        settingsLockEnabledState.value = enabled
                    },
                    onRequestDeviceAdmin = { requestDeviceAdmin() },
                    onRunSettingsLockChallenge = { lockType ->
                        settingsLockChallengeLauncher.launch(
                            Intent(this@BlockerControlActivity, LockChallengeActivity::class.java).apply {
                                putExtra(LockChallengeActivity.EXTRA_LOCK_TYPE, lockType)
                                putExtra(LockChallengeActivity.EXTRA_IS_SETTINGS_LOCK, true)
                            }
                        )
                    },
                    onShowAdultListDownload = { onFinished ->
                        showAdultListDownloadDialogAndEnsure(onFinished)
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(themeColor(R.attr.app_bg, Color(0xFFF2F4F6))),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = themeColor(R.attr.brand_accent, Color(0xFFFF6B6B))
                    )
                }
            }
        }
    }
}
