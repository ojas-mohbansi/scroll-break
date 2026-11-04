package com.cascade26.scrollbreak.Activities

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.TypedValue
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog as ComposeDialog
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.cascade26.scrollbreak.Common.IntentActions
import com.cascade26.scrollbreak.R
import com.cascade26.scrollbreak.Services.AppUsageAccessibilityService
import com.cascade26.scrollbreak.Utils.BlockedSitesManager
import com.cascade26.scrollbreak.Utils.ChallengeLockManager
import com.cascade26.scrollbreak.Utils.PerAppDailyLimitPreferences
import com.cascade26.scrollbreak.Utils.AppGroupDailyLimitSettings
import com.cascade26.scrollbreak.Utils.SettingsLockManager
import com.cascade26.scrollbreak.Utils.Utils
import com.cascade26.scrollbreak.db.dao.BlockedAppDao
import com.cascade26.scrollbreak.db.entities.BlockedAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockerControlScreen(
    onBackClick: () -> Unit,
    sharedPrefs: SharedPreferences,
    blockedAppDao: BlockedAppDao?,
    settingsLockMgr: SettingsLockManager,
    activityContext: FragmentActivity?,
    onShowAdultListDownload: ((Boolean) -> Unit) -> Unit,
    settingsLockEnabledValue: Boolean = settingsLockMgr.isLockEnabled,
    deviceAdminActive: Boolean = false,
    onSettingsLockChanged: (Boolean) -> Unit = { settingsLockMgr.isLockEnabled = it },
    onRequestDeviceAdmin: () -> Unit = {},
    onRunSettingsLockChallenge: (String) -> Unit = {}
) {
    val context = LocalContext.current

    val InterFamily = remember { FontFamily(Font(R.font.inter18regular)) }
    val PoppinsFamily = InterFamily

    val appBg = themeColor(R.attr.app_bg, Color(0xFFF2F4F6))
    val textPrimary = themeColor(R.attr.text_primary, Color(0xFF1F2937))
    val textTertiary = themeColor(R.attr.text_tertiary, Color(0xFF94A3B8))
    val brandPink = themeColor(R.attr.brand_accent, Color(0xFFFF6B6B))

    var isServicePaused by remember { mutableStateOf(sharedPrefs.getBoolean("isServicePaused", false)) }
    var blockerIntensity by remember { mutableIntStateOf(sharedPrefs.getInt(ChallengeLockManager.PREF_BLOCKER_INTENSITY, 0)) }
    var blockWebsites by remember { mutableStateOf(sharedPrefs.getBoolean(AppUsageAccessibilityService.PREF_BLOCK_BROWSERS_DOOMSCROLLING_ENABLED, false)) }
    var blockAdultContent by remember { mutableStateOf(sharedPrefs.getBoolean(AppUsageAccessibilityService.PREF_BLOCK_ADULT_SITES_ENABLED, false)) }
    var blockAds by remember { mutableStateOf(sharedPrefs.getBoolean("pref_block_ads", true)) }
    var enableVpn by remember { mutableStateOf(sharedPrefs.getBoolean("pref_vpn_enabled", false)) }
    var settingsLockEnabled by remember { mutableStateOf(settingsLockEnabledValue) }
    var selectedSettingsLockType by remember {
        mutableStateOf(sharedPrefs.getString(ChallengeLockManager.PREF_SETTINGS_LOCK_TYPE, "device") ?: "device")
    }
    LaunchedEffect(settingsLockEnabledValue) { settingsLockEnabled = settingsLockEnabledValue }

    val defaultDisplayApps = remember {
        listOf(
            BlockedAppEntity().apply { packageName = "com.google.android.youtube"; appName = "YouTube Shorts"; isRestricted = false; scope = "section"; useMod = false; sectionViewId = Utils.YtViewId },
            BlockedAppEntity().apply { packageName = "com.instagram.android"; appName = "Instagram Reels"; isRestricted = false; scope = "section"; useMod = false; sectionViewId = Utils.instaViewId },
            BlockedAppEntity().apply { packageName = "com.snapchat.android"; appName = "Snapchat Highlights"; isRestricted = false; scope = "section"; useMod = false; sectionViewId = Utils.snapViewId },
            BlockedAppEntity().apply { packageName = "com.facebook.katana"; appName = "Facebook"; isRestricted = false; scope = "section"; useMod = false; sectionViewId = Utils.facebookViewId },
            BlockedAppEntity().apply { packageName = "com.linkedin.android"; appName = "LinkedIn"; isRestricted = false; scope = "section"; useMod = false; sectionViewId = Utils.linkedinViewId },
            BlockedAppEntity().apply { packageName = "com.reddit.frontpage"; appName = "Reddit"; isRestricted = false; scope = "section"; useMod = false; sectionViewId = Utils.redditViewId },
            BlockedAppEntity().apply { packageName = "com.ss.android.ugc.trill"; appName = "TikTok"; isRestricted = false; scope = "section"; useMod = false; sectionViewId = Utils.tiktokViewId },
            BlockedAppEntity().apply { packageName = "com.twitter.android"; appName = "Twitter"; isRestricted = false; scope = "section"; useMod = false; sectionViewId = Utils.twitterViewId }
        )
    }
    var appsList by remember { mutableStateOf(defaultDisplayApps) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            blockedAppDao?.let { dao ->
                val allApps = dao.getAllSync()
                val displayApps = allApps.filter { !isModPackage(it.packageName) }
                if (displayApps.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        appsList = displayApps
                    }
                }
            }
        }
    }

    var showPauseDialog by remember { mutableStateOf(false) }
    var showNuclearWarningDialog by remember { mutableStateOf(false) }

    var pauseCountdownText by remember { mutableStateOf("") }
    LaunchedEffect(isServicePaused) {
        if (isServicePaused) {
            while (true) {
                val resumeTime = sharedPrefs.getLong("resumeTime", 0L)
                val remaining = resumeTime - System.currentTimeMillis()
                if (remaining <= 0) { isServicePaused = false; pauseCountdownText = ""; break }
                val h = remaining / 3_600_000L
                val m = (remaining / 60_000L) % 60
                val s = (remaining / 1_000L) % 60
                pauseCountdownText = "%02dh %02dm %02ds".format(h, m, s)
                delay(1000L.milliseconds)
            }
        } else {
            pauseCountdownText = ""
        }
    }

    fun notifyServiceConfigChanged() {
        activityContext?.let {
            val intent = Intent(IntentActions.getActionUpdatePackages(it)).apply {
                setPackage(it.packageName)
            }
            it.sendBroadcast(intent)
        }
    }

    fun setServicePauseState(paused: Boolean, duration: Long) {
        activityContext?.let {
            val intent = Intent(IntentActions.getActionPauseService(it)).apply {
                putExtra("pause_duration", duration)
                setPackage(it.packageName)
            }
            it.sendBroadcast(intent)
        }
        sharedPrefs.edit {
            putBoolean("isServicePaused", paused)
            putLong("resumeTime", if (paused) System.currentTimeMillis() + duration else 0)
        }
        isServicePaused = paused
    }

    fun saveIntensity(level: Int) {
        sharedPrefs.edit {
            putInt(ChallengeLockManager.PREF_BLOCKER_INTENSITY, level)
            when (level) {
                2 -> {
                    putBoolean(AppUsageAccessibilityService.PREF_REMIND_DOOM_SCROLLING_ENABLED, true)
                    putBoolean(AppUsageAccessibilityService.PREF_BLOCK_AFTER_WASTED_TIME_ENABLED, false)
                }
                3 -> {
                    putBoolean(AppUsageAccessibilityService.PREF_REMIND_DOOM_SCROLLING_ENABLED, true)
                    putBoolean(AppUsageAccessibilityService.PREF_BLOCK_AFTER_WASTED_TIME_ENABLED, true)
                }
                4 -> {
                    putBoolean(AppUsageAccessibilityService.PREF_REMIND_DOOM_SCROLLING_ENABLED, false)
                    putBoolean(AppUsageAccessibilityService.PREF_BLOCK_AFTER_WASTED_TIME_ENABLED, false)
                    putString(ChallengeLockManager.PREF_BLOCKER_CHALLENGE_TYPE, "oneday")
                }
                else -> {
                    putBoolean(AppUsageAccessibilityService.PREF_REMIND_DOOM_SCROLLING_ENABLED, false)
                    putBoolean(AppUsageAccessibilityService.PREF_BLOCK_AFTER_WASTED_TIME_ENABLED, false)
                }
            }
        }
        blockerIntensity = level
        notifyServiceConfigChanged()
    }

    fun enableSettingsLock() {
        onSettingsLockChanged(true)
        settingsLockEnabled = true
    }

    fun requireScrollBreakPassword(reason: String, onVerified: () -> Unit) {
        val activity = activityContext ?: return
        if (settingsLockMgr.hasCustomPin()) {
            settingsLockMgr.showVerifyPinDialog(activity, reason) { verified ->
                if (verified) onVerified()
                else Toast.makeText(activity, "Scroll Break password was not verified. Protection was not changed.", Toast.LENGTH_SHORT).show()
            }
        } else {
            settingsLockMgr.showSetCustomPinDialog(
                activity,
                false,
                {
                    if (settingsLockMgr.hasCustomPin()) onVerified()
                },
                {
                    Toast.makeText(activity, "Protection remains off until you create a Scroll Break password.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    fun showCustomPinFallback(reason: String) {
        val activity = activityContext ?: return
        AlertDialog.Builder(activity)
            .setTitle("Device authentication unavailable")
            .setMessage("$reason Set up a custom PIN to protect your settings instead?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Set up PIN") { _, _ ->
                settingsLockMgr.showSetCustomPinDialog(
                    activity,
                    false,
                    {
                        if (settingsLockMgr.hasCustomPin()) {
                            sharedPrefs.edit { putString(ChallengeLockManager.PREF_SETTINGS_LOCK_TYPE, SettingsLockManager.LOCK_TYPE_CUSTOM) }
                            selectedSettingsLockType = SettingsLockManager.LOCK_TYPE_CUSTOM
                            enableSettingsLock()
                        }
                    },
                    {
                        settingsLockEnabled = false
                    }
                )
            }
            .show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "APP BLOCKER",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = textTertiary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Blocker Control",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 28.sp,
                        color = textPrimary
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = themeColor(R.attr.surface_card, Color.White)
                ),
                border = BorderStroke(1.dp, themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(themeColor(R.attr.block_bg, Color(0xFFFFF1F2))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.shield),
                            contentDescription = "Blocker Status",
                            tint = brandPink,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Blocker Status",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = textPrimary
                        )
                        Text(
                            text = if (isServicePaused) {
                                if (pauseCountdownText.isNotEmpty()) "Paused — resumes in $pauseCountdownText"
                                else "Blocker is currently paused"
                            } else "Active protection enabled",
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                            color = textTertiary
                        )
                    }
                    Switch(
                        checked = !isServicePaused,
                        onCheckedChange = { checked ->
                            if (!checked) {
                                if (blockerIntensity == 4) {
                                    Toast.makeText(activityContext, "Cannot pause — Permanent lock is active.", Toast.LENGTH_SHORT).show()
                                } else if (settingsLockMgr.isLockEnabled && activityContext != null) {
                                    settingsLockMgr.authenticate(activityContext, "Pause Blocker Protection", object : SettingsLockManager.AuthCallback {
                                        override fun onSuccess() { showPauseDialog = true }
                                        override fun onFailure(reason: String?) {}
                                    })
                                } else {
                                    showPauseDialog = true
                                }
                            } else {
                                setServicePauseState(false, 0)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = brandPink,
                            uncheckedThumbColor = textTertiary,
                            uncheckedTrackColor = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            val coroutineScope = rememberCoroutineScope()
            fun reloadAppsList() {
                coroutineScope.launch(Dispatchers.IO) {
                    blockedAppDao?.let { dao ->
                        val allApps = dao.getAllSync()
                        val displayApps = allApps.filter { !isModPackage(it.packageName) }
                        withContext(Dispatchers.Main) {
                            appsList = displayApps
                        }
                    }
                }
            }

            RestrictedAppsGridSection(
                appsList = appsList,
                blockedAppDao = blockedAppDao,
                notifyService = { notifyServiceConfigChanged() },
                PoppinsFamily = PoppinsFamily,
                InterFamily = InterFamily,
                textPrimary = textPrimary,
                textTertiary = textTertiary,
                brandPink = brandPink,
                settingsLockMgr = settingsLockMgr,
                activityContext = activityContext,
                onAppUpdated = { updatedApp ->
                    appsList = appsList.map {
                        if (it.packageName == updatedApp.packageName) {
                            it.isRestricted = updatedApp.isRestricted
                            it.useMod = updatedApp.useMod
                        }
                        it
                    }.toList()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = themeColor(R.attr.surface_card, Color.White)
                ),
                border = BorderStroke(1.dp, themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Blocker Intensity",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (blockerIntensity) {
                            0 -> "NONE: Blocker is completely inactive."
                            1 -> "FRICTION: Complete a challenge to unlock temporarily."
                            2 -> "REMINDER: Popup warnings show periodically."
                            3 -> "TEMP LOCK: Hard lock when daily limit is reached."
                            4 -> "PERMANENT: Complete block with no challenge bypasses."
                            else -> ""
                        },
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = themeColor(R.attr.text_secondary, Color(0xFF64748B)),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    val intensityInteractionSource = remember { MutableInteractionSource() }
                    val isPressed by intensityInteractionSource.collectIsPressedAsState()
                    val isDragged by intensityInteractionSource.collectIsDraggedAsState()
                    val isSliderActive = isPressed || isDragged

                    val thumbHeight by animateDpAsState(
                        targetValue = if (isSliderActive) 30.dp else 24.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                    val trackHeight by animateDpAsState(
                        targetValue = if (isSliderActive) 18.dp else 16.dp,
                        animationSpec = tween(200)
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = blockerIntensity.toFloat(),
                            onValueChange = { valInt ->
                                blockerIntensity = valInt.toInt()
                            },
                            onValueChangeFinished = {
                                val currentIntensity = sharedPrefs.getInt(ChallengeLockManager.PREF_BLOCKER_INTENSITY, 0)
                                if (blockerIntensity != currentIntensity) {
                                    if (blockerIntensity < currentIntensity) {
                                        if (settingsLockMgr.isLockEnabled && activityContext != null) {
                                            settingsLockMgr.authenticate(activityContext, "Decrease Blocker Intensity", object : SettingsLockManager.AuthCallback {
                                                override fun onSuccess() {
                                                    if (blockerIntensity == 4) {
                                                        showNuclearWarningDialog = true
                                                    } else {
                                                        saveIntensity(blockerIntensity)
                                                    }
                                                }
                                                override fun onFailure(reason: String?) {
                                                    blockerIntensity = currentIntensity
                                                }
                                            })
                                        } else {
                                            if (blockerIntensity == 4) {
                                                showNuclearWarningDialog = true
                                            } else {
                                                saveIntensity(blockerIntensity)
                                            }
                                        }
                                    } else {
                                        if (blockerIntensity == 4) {
                                            showNuclearWarningDialog = true
                                        } else {
                                            saveIntensity(blockerIntensity)
                                        }
                                    }
                                }
                            },
                            valueRange = 0f..4f,
                            steps = 3,
                            interactionSource = intensityInteractionSource,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = brandPink,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent,
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            ),
                            track = { state ->
                                val fraction = state.coercedValueAsFraction
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(trackHeight)
                                        .clip(RoundedCornerShape(trackHeight / 2))
                                        .background(themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = fraction)
                                            .clip(RoundedCornerShape(trackHeight / 2))
                                            .background(brandPink)
                                    )
                                    if (fraction < 0.95f) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 8.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(brandPink)
                                        )
                                    }
                                }
                            },
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(width = 4.dp, height = thumbHeight)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(brandPink)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val labels = listOf("NONE", "FRICTION", "REMINDER", "TEMP LOCK", "PERMANENT")
                            labels.forEachIndexed { index, label ->
                                val isSelected = blockerIntensity == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            val currentIntensity = sharedPrefs.getInt(ChallengeLockManager.PREF_BLOCKER_INTENSITY, 0)
                                            if (index != currentIntensity) {
                                                if (index < currentIntensity) {
                                                    if (settingsLockMgr.isLockEnabled && activityContext != null) {
                                                        settingsLockMgr.authenticate(activityContext, "Decrease Blocker Intensity", object : SettingsLockManager.AuthCallback {
                                                            override fun onSuccess() {
                                                                if (index == 4) {
                                                                    showNuclearWarningDialog = true
                                                                } else {
                                                                    blockerIntensity = index
                                                                    saveIntensity(index)
                                                                }
                                                            }
                                                            override fun onFailure(reason: String?) {
                                                                blockerIntensity = currentIntensity
                                                            }
                                                        })
                                                    } else {
                                                        if (index == 4) {
                                                            showNuclearWarningDialog = true
                                                        } else {
                                                            blockerIntensity = index
                                                            saveIntensity(index)
                                                        }
                                                    }
                                                } else {
                                                    if (index == 4) {
                                                        showNuclearWarningDialog = true
                                                    } else {
                                                        blockerIntensity = index
                                                        saveIntensity(index)
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = PoppinsFamily,
                                        fontSize = 7.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) brandPink else textTertiary,
                                        modifier = Modifier.alpha(if (isSelected) 1f else 0.6f),
                                        textAlign = TextAlign.Center,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = blockerIntensity > 0,
                enter = expandVertically(animationSpec = tween(400)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = themeColor(R.attr.surface_nested, Color(0xFFF8FAFC))
                        ),
                        border = BorderStroke(1.dp, themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            when (blockerIntensity) {
                                1 -> {
                                    ChallengeSelectorSection(
                                        sharedPrefs = sharedPrefs,
                                        settingsLockMgr = settingsLockMgr,
                                        activityContext = activityContext,
                                        PoppinsFamily = PoppinsFamily,
                                        brandPink = brandPink,
                                        textPrimary = textPrimary,
                                        textTertiary = textTertiary,
                                        onChanged = { notifyServiceConfigChanged() }
                                    )
                                }
                                2 -> {
                                    ReminderIntervalSliders(
                                        sharedPrefs = sharedPrefs,
                                        settingsLockMgr = settingsLockMgr,
                                        activityContext = activityContext,
                                        PoppinsFamily = PoppinsFamily,
                                        InterFamily = InterFamily,
                                        brandPink = brandPink,
                                        textPrimary = textPrimary,
                                        textTertiary = textTertiary,
                                        onChanged = { notifyServiceConfigChanged() }
                                    )
                                }
                                3 -> {
                                    TempLockSliders(
                                        sharedPrefs = sharedPrefs,
                                        settingsLockMgr = settingsLockMgr,
                                        activityContext = activityContext,
                                        PoppinsFamily = PoppinsFamily,
                                        InterFamily = InterFamily,
                                        brandPink = brandPink,
                                        textPrimary = textPrimary,
                                        textTertiary = textTertiary,
                                        onChanged = { notifyServiceConfigChanged() }
                                    )
                                }
                                4 -> {
                                    Text(
                                        text = "⚠️ WARNING: Selected apps are permanently locked. You cannot bypass or disable blocker features while this mode is active.",
                                        fontFamily = InterFamily,
                                        fontSize = 13.sp,
                                        color = brandPink,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = themeColor(R.attr.surface_card, Color.White)
                ),
                border = BorderStroke(1.dp, themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (activityContext != null && !Utils.isAccessibilityPermissionGranted(activityContext)) {
                                    Toast.makeText(activityContext, "Accessibility permission is required.", Toast.LENGTH_SHORT).show()
                                } else if (activityContext != null) {
                                    val intent = ComponentName(activityContext, "com.cascade26.scrollbreak.Activities.SiteBlockerActivity")
                                    activityContext.startActivity(Intent().setComponent(intent))
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Web Blocker", fontFamily = PoppinsFamily, fontSize = 15.sp, color = textPrimary)
                            Text(
                                text = if (blockWebsites) "Block specified websites and urls (Tap to edit sites)" else "Block specified websites and urls",
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                color = textTertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = blockWebsites,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (activityContext != null && !Utils.isAccessibilityPermissionGranted(activityContext)) {
                                        Toast.makeText(activityContext, "Accessibility permission is required.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        sharedPrefs.edit {
                                            putBoolean(
                                                AppUsageAccessibilityService.PREF_BLOCK_BROWSERS_DOOMSCROLLING_ENABLED,
                                                true
                                            )
                                        }
                                        blockWebsites = true
                                        if (activityContext != null) {
                                            BlockedSitesManager.seedDefaultsIfFirstTimeAndEmpty(activityContext)
                                        }
                                        notifyServiceConfigChanged()
                                    }
                                } else {
                                    if (settingsLockMgr.isLockEnabled && activityContext != null) {
                                        settingsLockMgr.authenticate(activityContext, "Disable Web Blocker", object : SettingsLockManager.AuthCallback {
                                            override fun onSuccess() {
                                                sharedPrefs.edit {
                                                    putBoolean(
                                                        AppUsageAccessibilityService.PREF_BLOCK_BROWSERS_DOOMSCROLLING_ENABLED,
                                                        false
                                                    )
                                                }
                                                blockWebsites = false
                                                notifyServiceConfigChanged()
                                            }
                                            override fun onFailure(reason: String?) {}
                                        })
                                    } else {
                                        sharedPrefs.edit {
                                            putBoolean(
                                                AppUsageAccessibilityService.PREF_BLOCK_BROWSERS_DOOMSCROLLING_ENABLED,
                                                false
                                            )
                                        }
                                        blockWebsites = false
                                        notifyServiceConfigChanged()
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = brandPink,
                                uncheckedThumbColor = textTertiary,
                                uncheckedTrackColor = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))
                            )
                        )
                    }

                    HorizontalDivider(color = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)), modifier = Modifier.padding(horizontal = 16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = blockAdultContent) {
                                onShowAdultListDownload { }
                            }
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block Adult Sites", fontFamily = PoppinsFamily, fontSize = 15.sp, color = textPrimary)
                            Text(
                                text = if (blockAdultContent) "Filters adult urls automatically (Tap to update list)" else "Filters adult urls automatically",
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                color = textTertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = blockAdultContent,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (activityContext != null && !Utils.isAccessibilityPermissionGranted(activityContext)) {
                                        Toast.makeText(
                                            activityContext,
                                            "Accessibility permission is required.",
                                            Toast.LENGTH_SHORT).show()
                                    } else {
                                        onShowAdultListDownload { success ->
                                            if (success) {
                                                sharedPrefs.edit {putBoolean(AppUsageAccessibilityService.PREF_BLOCK_ADULT_SITES_ENABLED, true)}
                                                blockAdultContent = true
                                                notifyServiceConfigChanged()
                                            } else {
                                                sharedPrefs.edit {putBoolean(AppUsageAccessibilityService.PREF_BLOCK_ADULT_SITES_ENABLED, false)}
                                                blockAdultContent = false
                                            }
                                        }
                                    }
                                } else {
                                    if (settingsLockMgr.isLockEnabled && activityContext != null) {
                                        settingsLockMgr.authenticate(activityContext, "Disable Adult Sites Blocker", object : SettingsLockManager.AuthCallback {
                                            override fun onSuccess() {
                                                sharedPrefs.edit {putBoolean(AppUsageAccessibilityService.PREF_BLOCK_ADULT_SITES_ENABLED, false)}
                                                blockAdultContent = false
                                                notifyServiceConfigChanged()
                                            }
                                            override fun onFailure(reason: String?) {}
                                        })
                                    } else {
                                        sharedPrefs.edit {putBoolean(AppUsageAccessibilityService.PREF_BLOCK_ADULT_SITES_ENABLED, false)}
                                        blockAdultContent = false
                                        notifyServiceConfigChanged()
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = brandPink,
                                uncheckedThumbColor = textTertiary,
                                uncheckedTrackColor = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))
                            )
                        )
                    }

                    HorizontalDivider(color = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)), modifier = Modifier.padding(horizontal = 16.dp))

                    // Block Ads & Trackers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block Ads & Trackers (HaGeZ)", fontFamily = PoppinsFamily, fontSize = 15.sp, color = textPrimary)
                            Text("System-wide DNS filtering for ads and trackers", fontFamily = InterFamily, fontSize = 12.sp, color = textTertiary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = blockAds,
                            onCheckedChange = { checked ->
                                sharedPrefs.edit { putBoolean("pref_block_ads", checked) }
                                blockAds = checked
                                notifyServiceConfigChanged()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = brandPink,
                                uncheckedThumbColor = textTertiary,
                                uncheckedTrackColor = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))
                            )
                        )
                    }

                    HorizontalDivider(color = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)), modifier = Modifier.padding(horizontal = 16.dp))

                    // Enable DNS Protection VPN
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable DNS Protection VPN", fontFamily = PoppinsFamily, fontSize = 15.sp, color = textPrimary)
                            Text("Start local VPN service for system-wide blocking", fontFamily = InterFamily, fontSize = 12.sp, color = textTertiary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = enableVpn,
                            onCheckedChange = { checked ->
                                sharedPrefs.edit { putBoolean("pref_vpn_enabled", checked) }
                                enableVpn = checked
                                if (checked && activityContext != null) {
                                    val vpnIntent = android.net.VpnService.prepare(activityContext)
                                    if (vpnIntent != null) {
                                        activityContext.startActivity(vpnIntent)
                                    } else {
                                        activityContext.startService(Intent(activityContext, com.cascade26.scrollbreak.Utils.ScrollBrakeVpnService::class.java))
                                    }
                                } else if (activityContext != null) {
                                    activityContext.stopService(Intent(activityContext, com.cascade26.scrollbreak.Utils.ScrollBrakeVpnService::class.java))
                                }
                                notifyServiceConfigChanged()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = brandPink,
                                uncheckedThumbColor = textTertiary,
                                uncheckedTrackColor = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))
                            )
                        )
                    }
                }
            }
        }

        if (showPauseDialog) {
            ComposeDialog(onDismissRequest = {
                setServicePauseState(false, 0)
                showPauseDialog = false
            }) {
                var selectedHour by remember { mutableIntStateOf(0) }
                var selectedMinute by remember { mutableIntStateOf(30) }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = themeColor(R.attr.surface_card, Color.White)
                    ),
                    border = BorderStroke(1.dp, themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pause Blocker Protection",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = textPrimary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Hours", fontFamily = InterFamily, fontSize = 12.sp, color = textTertiary)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(themeColor(R.attr.brand_tint_bg, Color(0xFFFFF1F2)))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$selectedHour h",
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = brandPink
                                    )
                                }
                                Row {
                                    IconButton(onClick = { if (selectedHour > 0) selectedHour-- }) {
                                        Text("-", fontSize = 20.sp, color = textPrimary)
                                    }
                                    IconButton(onClick = { if (selectedHour < 23) selectedHour++ }) {
                                        Text("+", fontSize = 20.sp, color = textPrimary)
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Minutes", fontFamily = InterFamily, fontSize = 12.sp, color = textTertiary)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(themeColor(R.attr.brand_tint_bg, Color(0xFFFFF1F2)))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$selectedMinute m",
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = brandPink
                                    )
                                }
                                Row {
                                    IconButton(onClick = { if (selectedMinute >= 5) selectedMinute -= 5 }) {
                                        Text("-", fontSize = 20.sp, color = textPrimary)
                                    }
                                    IconButton(onClick = { if (selectedMinute <= 50) selectedMinute += 5 }) {
                                        Text("+", fontSize = 20.sp, color = textPrimary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    setServicePauseState(false, 0)
                                    showPauseDialog = false
                                }
                            ) {
                                Text("Cancel", color = textTertiary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    val pauseDuration = (selectedHour * 3600L + selectedMinute * 60L) * 1000L
                                    if (pauseDuration > 0) {
                                        setServicePauseState(true, pauseDuration)
                                        Toast.makeText(activityContext, "Blocker paused for ${selectedHour}h ${selectedMinute}m", Toast.LENGTH_SHORT).show()
                                        showPauseDialog = false
                                    } else {
                                        Toast.makeText(activityContext, "Please select a pause duration greater than 0 minutes.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = brandPink),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Pause Blocker", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (showNuclearWarningDialog) {
            var warnSecondsLeft by remember { mutableIntStateOf(5) }
            LaunchedEffect(Unit) {
                while (warnSecondsLeft > 0) {
                    delay(1000L.milliseconds)
                    warnSecondsLeft--
                }
            }

            ComposeDialog(onDismissRequest = {
                blockerIntensity = sharedPrefs.getInt(ChallengeLockManager.PREF_BLOCKER_INTENSITY, 0)
                showNuclearWarningDialog = false
            }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = themeColor(R.attr.surface_card, Color.White)
                    ),
                    border = BorderStroke(1.dp, themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "⚠️ Enable Permanent Block?",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = textPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = "Are you sure? Once Permanent lock level is active, you CANNOT bypass any locks, pauses, or change control settings. This lock is permanent until you manually change the intensity level.",
                            fontFamily = InterFamily,
                            fontSize = 14.sp,
                            color = themeColor(R.attr.text_secondary, Color(0xFF64748B)),
                            modifier = Modifier.padding(bottom = 24.dp),
                            lineHeight = 20.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    blockerIntensity = sharedPrefs.getInt(ChallengeLockManager.PREF_BLOCKER_INTENSITY, 0)
                                    showNuclearWarningDialog = false
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Cancel", color = textTertiary)
                            }

                            Button(
                                onClick = {
                                    saveIntensity(4)
                                    showNuclearWarningDialog = false
                                },
                                enabled = warnSecondsLeft <= 0,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brandPink,
                                    disabledContainerColor = brandPink.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (warnSecondsLeft > 0) "Understand (${warnSecondsLeft}s)" else "Understand",
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeSelectorSection(
    sharedPrefs: SharedPreferences,
    settingsLockMgr: SettingsLockManager,
    activityContext: FragmentActivity?,
    PoppinsFamily: FontFamily,
    brandPink: Color,
    textPrimary: Color,
    textTertiary: Color,
    onChanged: () -> Unit
) {
    var selectedChallenge by remember {
        mutableStateOf(sharedPrefs.getString(ChallengeLockManager.PREF_BLOCKER_CHALLENGE_TYPE, "math") ?: "math")
    }

    Text(
        text = "Challenge Type",
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = textPrimary,
        modifier = Modifier.padding(bottom = 10.dp)
    )

    val challenges = listOf("math", "shake", "scream", "breath")
    val labels = listOf("Math", "Shake", "Scream", "10 sec")
    val tabCount = challenges.size
    val selectedIndex = challenges.indexOf(selectedChallenge).coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)))
            .padding(2.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val indicatorWidth = maxWidth / tabCount
            val animatedOffset by animateDpAsState(
                targetValue = indicatorWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "challengeOffset"
            )

            Box(
                modifier = Modifier
                    .width(indicatorWidth)
                    .fillMaxHeight()
                    .offset(x = animatedOffset)
                    .clip(RoundedCornerShape(18.dp))
                    .background(brandPink)
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            challenges.forEachIndexed { i, key ->
                val isSelected = selectedChallenge == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (selectedChallenge != key) {
                                if (settingsLockMgr.isLockEnabled && activityContext != null) {
                                    settingsLockMgr.authenticate(activityContext, "Change Challenge Type", object : SettingsLockManager.AuthCallback {
                                        override fun onSuccess() {
                                            sharedPrefs.edit { putString(ChallengeLockManager.PREF_BLOCKER_CHALLENGE_TYPE, key) }
                                            selectedChallenge = key
                                            onChanged()
                                        }
                                        override fun onFailure(reason: String?) {}
                                    })
                                } else {
                                    sharedPrefs.edit { putString(ChallengeLockManager.PREF_BLOCKER_CHALLENGE_TYPE, key) }
                                    selectedChallenge = key
                                    onChanged()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labels[i],
                        fontFamily = PoppinsFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else textTertiary
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderIntervalSliders(
    sharedPrefs: SharedPreferences,
    settingsLockMgr: SettingsLockManager,
    activityContext: FragmentActivity?,
    PoppinsFamily: FontFamily,
    InterFamily: FontFamily,
    brandPink: Color,
    textPrimary: Color,
    textTertiary: Color,
    onChanged: () -> Unit
) {
    var intervalMinutes by remember {
        mutableIntStateOf(sharedPrefs.getInt(AppUsageAccessibilityService.PREF_REMIND_DOOM_SCROLLING_MINUTES, 5))
    }
    var popupSeconds by remember {
        mutableIntStateOf(sharedPrefs.getInt(AppUsageAccessibilityService.PREF_BLOCKING_POPUP_DURATION_SEC, 5))
    }
    var requireFriction by remember {
        mutableStateOf(sharedPrefs.getBoolean("pref_reminder_friction_enabled", false))
    }

    CustomLabelSlider(
        title = "Reminder Interval",
        subtitle = "Popup warning interval frequency",
        value = intervalMinutes,
        valueRange = 1f..60f,
        unit = "m",
        brandPink = brandPink,
        textPrimary = textPrimary,
        textTertiary = textTertiary,
        PoppinsFamily = PoppinsFamily,
        InterFamily = InterFamily,
        onValueChange = { intervalMinutes = it },
        onFinished = {
            sharedPrefs.edit {putInt(AppUsageAccessibilityService.PREF_REMIND_DOOM_SCROLLING_MINUTES, intervalMinutes)}
            onChanged()
        }
    )

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(color = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)))
    Spacer(modifier = Modifier.height(16.dp))

    CustomLabelSlider(
        title = "Popup Warning Duration",
        subtitle = "Display length before auto-dismiss",
        value = popupSeconds,
        valueRange = 3f..15f,
        unit = "s",
        brandPink = brandPink,
        textPrimary = textPrimary,
        textTertiary = textTertiary,
        PoppinsFamily = PoppinsFamily,
        InterFamily = InterFamily,
        onValueChange = { popupSeconds = it },
        onFinished = {
            sharedPrefs.edit {putInt(AppUsageAccessibilityService.PREF_BLOCKING_POPUP_DURATION_SEC, popupSeconds)}
            onChanged()
        }
    )

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(color = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)))
    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Require Friction on Open", fontFamily = PoppinsFamily, fontSize = 14.sp, color = textPrimary)
            Text("Force challenge when opening app", fontFamily = InterFamily, fontSize = 11.sp, color = textTertiary)
        }
        Switch(
            checked = requireFriction,
            onCheckedChange = { checked ->
                sharedPrefs.edit {putBoolean("pref_reminder_friction_enabled", checked)}
                requireFriction = checked
                onChanged()
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = brandPink,
                uncheckedThumbColor = textTertiary,
                uncheckedTrackColor = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))
            )
        )
    }

    if (requireFriction) {
        Spacer(modifier = Modifier.height(16.dp))
        ChallengeSelectorSection(
            sharedPrefs = sharedPrefs,
            settingsLockMgr = settingsLockMgr,
            activityContext = activityContext,
            PoppinsFamily = PoppinsFamily,
            brandPink = brandPink,
            textPrimary = textPrimary,
            textTertiary = textTertiary,
            onChanged = onChanged
        )
    }
}

@Composable
private fun AppGroupDailyLimitCard(
    displayName: String,
    initialSettings: AppGroupDailyLimitSettings,
    sharedPrefs: SharedPreferences,
    PoppinsFamily: FontFamily,
    InterFamily: FontFamily,
    brandPink: Color,
    textPrimary: Color,
    textTertiary: Color,
    onChanged: () -> Unit
) {
    var timeEnabled by remember(initialSettings.appTag) { mutableStateOf(initialSettings.timeEnabled) }
    var timeLimitMinutes by remember(initialSettings.appTag) { mutableIntStateOf(initialSettings.timeLimitMinutes) }
    var scrollEnabled by remember(initialSettings.appTag) { mutableStateOf(initialSettings.scrollEnabled) }
    var scrollLimit by remember(initialSettings.appTag) { mutableIntStateOf(initialSettings.scrollLimit) }

    fun persist() {
        PerAppDailyLimitPreferences.save(
            sharedPrefs,
            AppGroupDailyLimitSettings(
                appTag = initialSettings.appTag,
                timeEnabled = timeEnabled,
                timeLimitMinutes = timeLimitMinutes,
                scrollEnabled = scrollEnabled,
                scrollLimit = scrollLimit
            )
        )
        onChanged()
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = themeColor(R.attr.surface_card, Color.White)),
        border = BorderStroke(1.dp, themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = displayName,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = textPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily time limit", fontFamily = InterFamily, fontSize = 13.sp, color = textPrimary)
                    Text("1–240 minutes per day", fontFamily = InterFamily, fontSize = 11.sp, color = textTertiary)
                }
                Switch(
                    checked = timeEnabled,
                    onCheckedChange = { enabled ->
                        timeEnabled = enabled
                        persist()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = brandPink,
                        uncheckedThumbColor = textTertiary,
                        uncheckedTrackColor = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))
                    )
                )
            }
            AnimatedVisibility(visible = timeEnabled) {
                CustomLabelSlider(
                    title = "Time budget",
                    subtitle = "Daily total for $displayName and its variants",
                    value = timeLimitMinutes,
                    valueRange = 1f..240f,
                    unit = "m",
                    brandPink = brandPink,
                    textPrimary = textPrimary,
                    textTertiary = textTertiary,
                    PoppinsFamily = PoppinsFamily,
                    InterFamily = InterFamily,
                    onValueChange = { timeLimitMinutes = it },
                    onFinished = { persist() }
                )
            }

            HorizontalDivider(color = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily scroll limit", fontFamily = InterFamily, fontSize = 13.sp, color = textPrimary)
                    Text("10–500 feed scrolls per day", fontFamily = InterFamily, fontSize = 11.sp, color = textTertiary)
                }
                Switch(
                    checked = scrollEnabled,
                    onCheckedChange = { enabled ->
                        scrollEnabled = enabled
                        persist()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = brandPink,
                        uncheckedThumbColor = textTertiary,
                        uncheckedTrackColor = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))
                    )
                )
            }
            AnimatedVisibility(visible = scrollEnabled) {
                CustomLabelSlider(
                    title = "Scroll budget",
                    subtitle = "Daily total for $displayName and its variants",
                    value = scrollLimit,
                    valueRange = 10f..500f,
                    unit = " scrolls",
                    brandPink = brandPink,
                    textPrimary = textPrimary,
                    textTertiary = textTertiary,
                    PoppinsFamily = PoppinsFamily,
                    InterFamily = InterFamily,
                    onValueChange = { scrollLimit = it },
                    onFinished = { persist() }
                )
            }
        }
    }
}

@Composable
fun TempLockSliders(
    sharedPrefs: SharedPreferences,
    settingsLockMgr: SettingsLockManager,
    activityContext: FragmentActivity?,
    PoppinsFamily: FontFamily,
    InterFamily: FontFamily,
    brandPink: Color,
    textPrimary: Color,
    textTertiary: Color,
    onChanged: () -> Unit
) {
    var requireReminders by remember {
        mutableStateOf(sharedPrefs.getBoolean("pref_temp_lock_reminders_enabled", false))
    }
    var requireFriction by remember {
        mutableStateOf(sharedPrefs.getBoolean("pref_temp_lock_friction_enabled", false))
    }

    Text(
        text = "Daily Limits by App",
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = textPrimary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Text(
        text = "Each app group has its own time and scroll budgets. Variants share the same budget.",
        fontFamily = InterFamily,
        fontSize = 12.sp,
        color = textTertiary,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    PerAppDailyLimitPreferences.APP_GROUPS.forEach { appGroup ->
        AppGroupDailyLimitCard(
            displayName = appGroup.displayName,
            initialSettings = PerAppDailyLimitPreferences.settings(sharedPrefs, appGroup.tag),
            sharedPrefs = sharedPrefs,
            PoppinsFamily = PoppinsFamily,
            InterFamily = InterFamily,
            brandPink = brandPink,
            textPrimary = textPrimary,
            textTertiary = textTertiary,
            onChanged = onChanged
        )
        Spacer(modifier = Modifier.height(10.dp))
    }

    HorizontalDivider(color = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)))
    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Show Reminder Popups", fontFamily = PoppinsFamily, fontSize = 14.sp, color = textPrimary)
            Text("Popup warning alerts show periodically under limit", fontFamily = InterFamily, fontSize = 11.sp, color = textTertiary)
        }
        Switch(
            checked = requireReminders,
            onCheckedChange = { checked ->
                sharedPrefs.edit {putBoolean("pref_temp_lock_reminders_enabled", checked)}
                requireReminders = checked
                onChanged()
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = brandPink,
                uncheckedThumbColor = textTertiary,
                uncheckedTrackColor = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))
            )
        )
    }

    if (requireReminders) {
        Spacer(modifier = Modifier.height(16.dp))
        ReminderIntervalSliders(
            sharedPrefs = sharedPrefs,
            settingsLockMgr = settingsLockMgr,
            activityContext = activityContext,
            PoppinsFamily = PoppinsFamily,
            InterFamily = InterFamily,
            brandPink = brandPink,
            textPrimary = textPrimary,
            textTertiary = textTertiary,
            onChanged = onChanged
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(color = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)))
    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Require Friction on Open", fontFamily = PoppinsFamily, fontSize = 14.sp, color = textPrimary)
            Text("Force challenge when opening under limit", fontFamily = InterFamily, fontSize = 11.sp, color = textTertiary)
        }
        Switch(
            checked = requireFriction,
            onCheckedChange = { checked ->
                sharedPrefs.edit {putBoolean("pref_temp_lock_friction_enabled", checked)}
                requireFriction = checked
                onChanged()
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = brandPink,
                uncheckedThumbColor = textTertiary,
                uncheckedTrackColor = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))
            )
        )
    }

    if (requireFriction) {
        Spacer(modifier = Modifier.height(16.dp))
        ChallengeSelectorSection(
            sharedPrefs = sharedPrefs,
            settingsLockMgr = settingsLockMgr,
            activityContext = activityContext,
            PoppinsFamily = PoppinsFamily,
            brandPink = brandPink,
            textPrimary = textPrimary,
            textTertiary = textTertiary,
            onChanged = onChanged
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLabelSlider(
    title: String,
    subtitle: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    brandPink: Color,
    textPrimary: Color,
    textTertiary: Color,
    PoppinsFamily: FontFamily,
    InterFamily: FontFamily,
    onValueChange: (Int) -> Unit,
    onFinished: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = textPrimary
                )
                Text(
                    text = subtitle,
                    fontFamily = InterFamily,
                    fontSize = 11.sp,
                    color = textTertiary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColor(R.attr.brand_tint_bg, Color(0xFFFFF1F2)))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$value$unit",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = brandPink
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val isDragged by interactionSource.collectIsDraggedAsState()
        val isSliderActive = isPressed || isDragged

        val thumbHeight by animateDpAsState(
            targetValue = if (isSliderActive) 30.dp else 24.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )
        val trackHeight by animateDpAsState(
            targetValue = if (isSliderActive) 18.dp else 16.dp,
            animationSpec = tween(200)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${valueRange.start.toInt()}",
                fontFamily = PoppinsFamily,
                fontSize = 11.sp,
                color = textTertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                onValueChangeFinished = onFinished,
                valueRange = valueRange,
                interactionSource = interactionSource,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                colors = SliderDefaults.colors(
                    thumbColor = brandPink,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                track = { state ->
                    val fraction = state.coercedValueAsFraction
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(trackHeight)
                            .clip(RoundedCornerShape(trackHeight / 2))
                            .background(themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = fraction)
                                .clip(RoundedCornerShape(trackHeight / 2))
                                .background(brandPink)
                        )
                        if (fraction < 0.95f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 8.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(brandPink)
                            )
                        }
                    }
                },
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = thumbHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(brandPink)
                    )
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${valueRange.endInclusive.toInt()}",
                fontFamily = PoppinsFamily,
                fontSize = 11.sp,
                color = textTertiary
            )
        }
    }
}

@Composable
fun BlockedAppRowItem(
    app: BlockedAppEntity,
    blockedAppDao: BlockedAppDao?,
    notifyService: () -> Unit,
    PoppinsFamily: FontFamily,
    InterFamily: FontFamily,
    textPrimary: Color,
    textTertiary: Color,
    brandPink: Color,
    hasDivider: Boolean
) {
    val context = LocalContext.current
    val sharedPrefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val scope = rememberCoroutineScope()
    var isRestricted by remember { mutableStateOf(app.isRestricted) }
    var scopeSetting by remember { mutableStateOf(app.scope ?: "full") }
    var useModSetting by remember { mutableStateOf(app.useMod) }

    fun syncModPackagesLocal(parentApp: BlockedAppEntity) {
        scope.launch(Dispatchers.IO) {
            blockedAppDao?.let { dao ->
                val allApps = dao.getAllSync()
                for (a in allApps) {
                    if (isModPackage(a.packageName)) {
                        var match = false
                        if (parentApp.packageName == "com.google.android.youtube" && a.packageName.contains("youtube")) {
                            match = true
                        } else if (parentApp.packageName == "com.instagram.android" && (a.packageName.contains("insta") || a.packageName.contains("honista"))) {
                            match = true
                        } else if (parentApp.packageName == "com.facebook.katana" && a.packageName == "com.facebook.lite") {
                            match = true
                        } else if (parentApp.packageName == "com.ss.android.ugc.trill" &&
                            (a.packageName == "com.zhiliaoapp.musically" ||
                             a.packageName == "com.ss.android.ugc.aweme" ||
                             a.packageName == "com.ss.android.ugc.aweme.lite")) {
                            match = true
                        } else if (parentApp.packageName == "com.reddit.frontpage" &&
                            (a.packageName == "com.andrewshu.android.reddit" ||
                             a.packageName == "ml.docilealligator.infinityforreddit" ||
                             a.packageName == "free.reddit.news" ||
                             a.packageName == "com.laurencedawson.reddit_sync" ||
                             a.packageName == "com.reddit.frontpage.lite")) {
                            match = true
                        } else if (parentApp.packageName == "com.twitter.android" && a.packageName == "com.twitter.android.lite") {
                            match = true
                        }
                        if (match) {
                            a.isRestricted = parentApp.isRestricted && parentApp.useMod
                            a.scope = parentApp.scope
                            a.useMod = parentApp.useMod
                            dao.update(a)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        if (hasDivider) {
            HorizontalDivider(
                color = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(packageName = app.packageName)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = textPrimary
                )
            }

            Switch(
                checked = isRestricted,
                onCheckedChange = { checked ->
                    if (checked) {
                        isRestricted = true
                        app.isRestricted = true
                        sharedPrefs.edit {
                            when (app.packageName) {
                                "com.google.android.youtube", "com.rvx.android.youtube", "com.revance.android.youtube", "app.morphe.android.youtube" -> {
                                    putBoolean("ytSwitchState", true)
                                }
                                "com.instagram.android", "com.instagram.lite", "com.myinsta.android", "com.instafel.android", "com.instander.android", "com.instagold.android", "com.instapro2.android", "com.instaflow.android", "cc.honista.app", "com.instaprime.android" -> {
                                    putBoolean("instaSwitchState", true)
                                }
                                "com.snapchat.android" -> {
                                    putBoolean("snapSwitchState", true)
                                }
                                "com.facebook.katana" -> { putBoolean("facebookSwitchState", true) }
                                "com.linkedin.android" -> { putBoolean("linkedinSwitchState", true) }
                                "com.reddit.frontpage" -> { putBoolean("redditSwitchState", true) }
                                "com.ss.android.ugc.trill" -> { putBoolean("tiktokSwitchState", true) }
                                "com.twitter.android" -> { putBoolean("twitterSwitchState", true) }
                            }
                        }
                        scope.launch(Dispatchers.IO) {
                            blockedAppDao?.update(app)
                            syncModPackagesLocal(app)
                            withContext(Dispatchers.Main) {
                                notifyService()
                            }
                        }
                    } else {
                        val settingsLockMgr = SettingsLockManager(context)
                        val activityContext = context as? FragmentActivity
                        if (settingsLockMgr.isLockEnabled && activityContext != null) {
                            settingsLockMgr.authenticate(activityContext, "Remove App Restriction", object : SettingsLockManager.AuthCallback {
                                override fun onSuccess() {
                                    isRestricted = false
                                    app.isRestricted = false
                                    sharedPrefs.edit {
                                        when (app.packageName) {
                                            "com.google.android.youtube", "com.rvx.android.youtube", "com.revance.android.youtube", "app.morphe.android.youtube" -> {
                                                putBoolean("ytSwitchState", false)
                                            }
                                            "com.instagram.android", "com.instagram.lite", "com.myinsta.android", "com.instafel.android", "com.instander.android", "com.instagold.android", "com.instapro2.android", "com.instaflow.android", "cc.honista.app", "com.instaprime.android" -> {
                                                putBoolean("instaSwitchState", false)
                                            }
                                            "com.snapchat.android" -> {
                                                putBoolean("snapSwitchState", false)
                                            }
                                            "com.facebook.katana" -> { putBoolean("facebookSwitchState", false) }
                                            "com.linkedin.android" -> { putBoolean("linkedinSwitchState", false) }
                                            "com.reddit.frontpage" -> { putBoolean("redditSwitchState", false) }
                                            "com.ss.android.ugc.trill" -> { putBoolean("tiktokSwitchState", false) }
                                            "com.twitter.android" -> { putBoolean("twitterSwitchState", false) }
                                        }
                                    }
                                    scope.launch(Dispatchers.IO) {
                                        blockedAppDao?.update(app)
                                        syncModPackagesLocal(app)
                                        withContext(Dispatchers.Main) {
                                            notifyService()
                                        }
                                    }
                                }
                                override fun onFailure(reason: String?) {}
                            })
                        } else {
                            isRestricted = false
                            app.isRestricted = false
                            sharedPrefs.edit {
                                when (app.packageName) {
                                    "com.google.android.youtube", "com.rvx.android.youtube", "com.revance.android.youtube", "app.morphe.android.youtube" -> {
                                        putBoolean("ytSwitchState", false)
                                    }
                                    "com.instagram.android", "com.instagram.lite", "com.myinsta.android", "com.instafel.android", "com.instander.android", "com.instagold.android", "com.instapro2.android", "com.instaflow.android", "cc.honista.app", "com.instaprime.android" -> {
                                        putBoolean("instaSwitchState", false)
                                    }
                                    "com.snapchat.android" -> {
                                        putBoolean("snapSwitchState", false)
                                    }
                                    "com.facebook.katana" -> { putBoolean("facebookSwitchState", false) }
                                    "com.linkedin.android" -> { putBoolean("linkedinSwitchState", false) }
                                    "com.reddit.frontpage" -> { putBoolean("redditSwitchState", false) }
                                    "com.ss.android.ugc.trill" -> { putBoolean("tiktokSwitchState", false) }
                                    "com.twitter.android" -> { putBoolean("twitterSwitchState", false) }
                                }
                            }
                            scope.launch(Dispatchers.IO) {
                                blockedAppDao?.update(app)
                                syncModPackagesLocal(app)
                                withContext(Dispatchers.Main) {
                                    notifyService()
                                }
                            }
                        }
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = brandPink,
                    uncheckedThumbColor = textTertiary,
                    uncheckedTrackColor = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))
                )
            )
        }
    }
}

@Composable
fun AppIconImage(packageName: String) {
    val context = LocalContext.current
    var drawableIcon by remember { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            try {
                val icon = context.packageManager.getApplicationIcon(packageName)
                withContext(Dispatchers.Main) {
                    drawableIcon = icon
                }
            } catch (_: Exception) {}
        }
    }

    if (drawableIcon != null) {
        val imageBitmap = remember(drawableIcon) {
            val bitmap = createBitmap(
                drawableIcon!!.intrinsicWidth.coerceAtLeast(1),
                drawableIcon!!.intrinsicHeight.coerceAtLeast(1)
            )
            val canvas = Canvas(bitmap)
            drawableIcon!!.setBounds(0, 0, canvas.width, canvas.height)
            drawableIcon!!.draw(canvas)
            bitmap.asImageBitmap()
        }
        Image(
            bitmap = imageBitmap,
            contentDescription = "App Icon",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
    } else {
        val iconRes = when {
            packageName.contains("youtube")   -> R.drawable.youtube
            packageName.contains("insta")     -> R.drawable.instagram
            packageName.contains("snap")      -> R.drawable.snapchat
            packageName.contains("katana") || packageName.contains("facebook") -> R.drawable.facebook
            packageName.contains("linkedin")  -> R.drawable.linkedin
            packageName.contains("reddit") || packageName.contains("andrewshu") ||
                packageName.contains("infinityforreddit") || packageName.contains("reddit_sync") ||
                packageName == "free.reddit.news" -> R.drawable.reddit
            packageName.contains("trill") || packageName.contains("musically") ||
                packageName.contains("ugc.aweme") -> R.drawable.tiktok
            packageName.contains("twitter")  -> R.drawable.twitter
            else -> R.drawable.shield
        }

        val iconColor = when {
            packageName.contains("youtube")   -> Color(0xFFFF0000)
            packageName.contains("insta")     -> Color(0xFFE1306C)
            packageName.contains("snap")      -> Color(0xFFFFFC00)
            packageName.contains("katana") || packageName.contains("facebook") -> Color(0xFF1877F2)
            packageName.contains("linkedin")  -> Color(0xFF0A66C2)
            packageName.contains("reddit") || packageName.contains("andrewshu") ||
                packageName.contains("infinityforreddit") || packageName.contains("reddit_sync") ||
                packageName == "free.reddit.news" -> Color(0xFFFF4500)
            packageName.contains("trill") || packageName.contains("musically") ||
                packageName.contains("ugc.aweme") -> Color(0xFF010101)
            packageName.contains("twitter")  -> Color(0xFF1DA1F2)
            else -> themeColor(R.attr.text_secondary, Color(0xFF64748B))
        }

        val bgTint = when {
            packageName.contains("youtube")   -> Color(0xFFFF0000).copy(alpha = 0.2f)
            packageName.contains("insta")     -> Color(0xFFE1306C).copy(alpha = 0.2f)
            packageName.contains("snap")      -> Color(0xFFFFFC00).copy(alpha = 0.2f)
            packageName.contains("katana") || packageName.contains("facebook") -> Color(0xFF1877F2).copy(alpha = 0.15f)
            packageName.contains("linkedin")  -> Color(0xFF0A66C2).copy(alpha = 0.15f)
            packageName.contains("reddit") || packageName.contains("andrewshu") ||
                packageName.contains("infinityforreddit") || packageName.contains("reddit_sync") ||
                packageName == "free.reddit.news" -> Color(0xFFFF4500).copy(alpha = 0.15f)
            packageName.contains("trill") || packageName.contains("musically") ||
                packageName.contains("ugc.aweme") -> Color(0xFF69C9D0).copy(alpha = 0.2f)
            packageName.contains("twitter")  -> Color(0xFF1DA1F2).copy(alpha = 0.15f)
            else -> Color.White.copy(alpha = 0.1f)
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(bgTint)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = "App Icon Fallback",
                tint = iconColor,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

fun isModPackage(packageName: String?): Boolean {
    if (packageName == null) return false
    return packageName == "com.rvx.android.youtube" ||
           packageName == "com.revance.android.youtube" ||
           packageName == "app.morphe.android.youtube" ||
           packageName == "com.myinsta.android" ||
           packageName == "com.instafel.android" ||
           packageName == "com.instander.android" ||
           packageName == "com.instagold.android" ||
           packageName == "com.instapro2.android" ||
           packageName == "com.instaflow.android" ||
           packageName == "cc.honista.app" ||
           packageName == "com.instaprime.android" ||
           packageName == "com.instagram.lite" ||
           packageName == "com.facebook.lite" ||
           packageName == "com.zhiliaoapp.musically" ||
           packageName == "com.ss.android.ugc.aweme" ||
           packageName == "com.ss.android.ugc.aweme.lite" ||
           packageName == "com.andrewshu.android.reddit" ||
           packageName == "ml.docilealligator.infinityforreddit" ||
           packageName == "free.reddit.news" ||
           packageName == "com.laurencedawson.reddit_sync" ||
           packageName == "com.reddit.frontpage.lite" ||
           packageName == "com.twitter.android.lite"
}

@Preview(showBackground = true)
@Composable
fun BlockerControlScreenPreview() {
    BlockerControlScreen(
        onBackClick = {},
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(LocalContext.current),
        blockedAppDao = null,
        settingsLockMgr = SettingsLockManager(LocalContext.current),
        activityContext = null,
        onShowAdultListDownload = {}
    )
}

@Composable
fun SettingsLockTypeSelectorSection(
    sharedPrefs: SharedPreferences,
    settingsLockMgr: SettingsLockManager,
    activityContext: FragmentActivity?,
    selectedLockType: String,
    onLockTypeSelected: (String) -> Unit,
    PoppinsFamily: FontFamily,
    InterFamily: FontFamily,
    brandPink: Color,
    textPrimary: Color,
    textTertiary: Color,
    onChanged: () -> Unit
) {
    var selectedType by remember(selectedLockType) { mutableStateOf(selectedLockType) }

    val options = listOf("device", "custom", "math", "text", "oneday")
    val labels = listOf("Device credentials", "Custom PIN", "Maths Challenge", "Long sentences", "1-Day Lockout")

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = "Select Lock Type",
            fontFamily = PoppinsFamily,
            fontSize = 12.sp,
            color = textTertiary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(themeColor(R.attr.surface_nested, Color(0xFFF8FAFC)))
                .padding(4.dp)
        ) {
            options.forEachIndexed { index, type ->
                val isSelected = selectedType == type
                val handleTypeSelection = {
                    if (selectedType != type) {
                        if (settingsLockMgr.isLockEnabled && activityContext != null) {
                            settingsLockMgr.authenticate(activityContext, "Change Settings Lock Type", object : SettingsLockManager.AuthCallback {
                                override fun onSuccess() {
                                    if (type == "custom" && !settingsLockMgr.hasCustomPin()) {
                                        settingsLockMgr.showSetCustomPinDialog(activityContext, false) {
                                            if (settingsLockMgr.hasCustomPin()) {
                                                sharedPrefs.edit { putString(ChallengeLockManager.PREF_SETTINGS_LOCK_TYPE, "custom") }
                                                selectedType = "custom"
                                                onLockTypeSelected("custom")
                                                onChanged()
                                            }
                                        }
                                    } else {
                                        sharedPrefs.edit { putString(ChallengeLockManager.PREF_SETTINGS_LOCK_TYPE, type) }
                                        selectedType = type
                                        onLockTypeSelected(type)
                                        onChanged()
                                    }
                                }
                                override fun onFailure(reason: String?) {}
                            })
                        } else {
                            if (type == "custom" && !settingsLockMgr.hasCustomPin() && activityContext != null) {
                                settingsLockMgr.showSetCustomPinDialog(activityContext, false) {
                                    if (settingsLockMgr.hasCustomPin()) {
                                        sharedPrefs.edit { putString(ChallengeLockManager.PREF_SETTINGS_LOCK_TYPE, "custom") }
                                        selectedType = "custom"
                                        onLockTypeSelected("custom")
                                        onChanged()
                                    }
                                }
                            } else {
                                sharedPrefs.edit { putString(ChallengeLockManager.PREF_SETTINGS_LOCK_TYPE, type) }
                                selectedType = type
                                onLockTypeSelected(type)
                                onChanged()
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) brandPink.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { handleTypeSelection() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { handleTypeSelection() },
                        colors = RadioButtonDefaults.colors(selectedColor = brandPink)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = labels[index],
                            fontFamily = PoppinsFamily,
                            fontSize = 14.sp,
                            color = textPrimary
                        )
                        val subtitle = when (type) {
                            "device" -> "Use device PIN, pattern or biometric fingerprint"
                            "custom" -> "Use a custom 6-digit PIN specific to Scroll Break"
                            "math" -> "Solve a complex equation to gain access"
                            "text" -> "Type a long quote exactly to bypass"
                            "oneday" -> "Strict lockout: 24h cooldown timer on settings"
                            else -> ""
                        }
                        Text(
                            text = subtitle,
                            fontFamily = InterFamily,
                            fontSize = 11.sp,
                            color = textTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun themeColor(attr: Int, default: Color): Color {
    val context = LocalContext.current
    val typedValue = remember { TypedValue() }
    val resolved = context.theme.resolveAttribute(attr, typedValue, true)
    return if (resolved) {
        Color(typedValue.data)
    } else {
        default
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: BlockedAppEntity,
    PoppinsFamily: FontFamily,
    InterFamily: FontFamily,
    textPrimary: Color,
    textTertiary: Color,
    brandPink: Color,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val isRestricted = app.isRestricted

    val brandColor = remember(app.packageName) {
        when {
            app.packageName.contains("youtube")   -> Color(0xFFFF0000)
            app.packageName.contains("insta")     -> Color(0xFFE1306C)
            app.packageName.contains("snap")      -> Color(0xFFFFFC00)
            app.packageName.contains("katana") || app.packageName.contains("facebook") -> Color(0xFF1877F2)
            app.packageName.contains("linkedin")  -> Color(0xFF0A66C2)
            app.packageName.contains("reddit")    -> Color(0xFFFF4500)
            app.packageName.contains("trill") || app.packageName.contains("musically") ||
                app.packageName.contains("ugc.aweme") -> Color(0xFF101010)
            app.packageName.contains("twitter")  -> Color(0xFF1DA1F2)
            else -> Color(0xFF64748B)
        }
    }

    val iconRes = remember(app.packageName) {
        when {
            app.packageName.contains("youtube")   -> R.drawable.youtube
            app.packageName.contains("insta")     -> R.drawable.instagram
            app.packageName.contains("snap")      -> R.drawable.snapchat
            app.packageName.contains("katana") || app.packageName.contains("facebook") -> R.drawable.facebook
            app.packageName.contains("linkedin")  -> R.drawable.linkedin
            app.packageName.contains("reddit")    -> R.drawable.reddit
            app.packageName.contains("trill") || app.packageName.contains("musically") ||
                app.packageName.contains("ugc.aweme") -> R.drawable.tiktok
            app.packageName.contains("twitter")  -> R.drawable.twitter
            else -> R.drawable.shield
        }
    }

    val borderCol = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))

    val bgCol by animateColorAsState(
        targetValue = if (isRestricted) brandColor else Color.Transparent,
        label = "bg"
    )

    val iconTint by animateColorAsState(
        targetValue = when {
            !isRestricted -> textTertiary
            app.packageName.contains("snap") -> Color(0xFF1A1A1A)
            else -> Color.White
        },
        label = "iconTint"
    )

    val displayName = remember(app.packageName) {
        when (app.packageName) {
            "com.google.android.youtube" -> "YouTube"
            "com.instagram.android" -> "Instagram"
            "com.snapchat.android" -> "Snapchat"
            "com.facebook.katana" -> "Facebook"
            "com.linkedin.android" -> "LinkedIn"
            "com.reddit.frontpage" -> "Reddit"
            "com.ss.android.ugc.trill" -> "TikTok"
            "com.twitter.android" -> "Twitter"
            else -> app.appName ?: ""
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .padding(vertical = 8.dp)
            .scale(scale.value)
            .combinedClickable(
                onClick = {
                    scope.launch {
                        scale.animateTo(0.9f, spring(stiffness = Spring.StiffnessHigh))
                        scale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium))
                    }
                    onClick()
                },
                onLongClick = onLongClick
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgCol)
                    .border(1.2.dp, borderCol, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = displayName,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(iconTint),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isRestricted) {
                val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "rotate")
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = "Expand Scope Selection",
                    tint = textTertiary,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 2.dp)
                        .size(16.dp)
                        .graphicsLayer { rotationZ = rotation }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onExpandClick()
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = displayName,
            fontFamily = InterFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun PackageItemRow(
    item: BlockedAppEntity,
    category: String,
    isInstalled: Boolean,
    InterFamily: FontFamily,
    PoppinsFamily: FontFamily,
    textPrimary: Color,
    textTertiary: Color,
    brandPink: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when (category) {
                        "Original" -> Color(0xFF1E88E5).copy(alpha = 0.1f)
                        "Lite" -> Color(0xFF00ACC1).copy(alpha = 0.1f)
                        else -> Color(0xFFD81B60).copy(alpha = 0.1f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.shield),
                contentDescription = null,
                tint = when (category) {
                    "Original" -> Color(0xFF1E88E5)
                    "Lite" -> Color(0xFF00ACC1)
                    else -> Color(0xFFD81B60)
                },
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = remember(item.packageName) {
                        val simpleName = item.packageName.substringAfterLast('.')
                        simpleName.replaceFirstChar { it.uppercase() }
                    },
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (category) {
                                "Original" -> Color(0xFF1E88E5).copy(alpha = 0.15f)
                                "Lite" -> Color(0xFF00ACC1).copy(alpha = 0.15f)
                                else -> Color(0xFFD81B60).copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = category,
                        fontFamily = InterFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (category) {
                            "Original" -> Color(0xFF1E88E5)
                            "Lite" -> Color(0xFF00ACC1)
                            else -> Color(0xFFD81B60)
                        }
                    )
                }
            }
            Text(
                text = item.packageName,
                fontFamily = InterFamily,
                fontSize = 10.sp,
                color = textTertiary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (item.isRestricted) brandPink else Color(0xFF4CAF50))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (item.isRestricted) "Blocked" else "Allowed",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (item.isRestricted) brandPink else Color(0xFF4CAF50)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isInstalled) "Installed" else "Not Installed",
                fontFamily = InterFamily,
                fontSize = 9.sp,
                color = if (isInstalled) Color(0xFF4CAF50) else textTertiary
            )
        }
    }
}

@Composable
fun PackageDetailDialog(
    parentApp: BlockedAppEntity,
    blockedAppDao: BlockedAppDao?,
    PoppinsFamily: FontFamily,
    InterFamily: FontFamily,
    textPrimary: Color,
    textTertiary: Color,
    brandPink: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var packageEntities by remember { mutableStateOf<List<BlockedAppEntity>>(emptyList()) }

    LaunchedEffect(parentApp) {
        withContext(Dispatchers.IO) {
            blockedAppDao?.let { dao ->
                val allApps = dao.getAllSync()
                val matched = mutableListOf<BlockedAppEntity>()
                matched.add(parentApp)

                for (a in allApps) {
                    if (isModPackage(a.packageName)) {
                        var match = false
                        if (parentApp.packageName == "com.google.android.youtube" && a.packageName.contains("youtube")) {
                            match = true
                        } else if (parentApp.packageName == "com.instagram.android" && (a.packageName.contains("insta") || a.packageName.contains("honista"))) {
                            match = true
                        } else if (parentApp.packageName == "com.facebook.katana" && a.packageName == "com.facebook.lite") {
                            match = true
                        } else if (parentApp.packageName == "com.ss.android.ugc.trill" &&
                            (a.packageName == "com.zhiliaoapp.musically" ||
                             a.packageName == "com.ss.android.ugc.aweme" ||
                             a.packageName == "com.ss.android.ugc.aweme.lite")) {
                            match = true
                        } else if (parentApp.packageName == "com.reddit.frontpage" &&
                            (a.packageName == "com.andrewshu.android.reddit" ||
                             a.packageName == "ml.docilealligator.infinityforreddit" ||
                             a.packageName == "free.reddit.news" ||
                             a.packageName == "com.laurencedawson.reddit_sync" ||
                             a.packageName == "com.reddit.frontpage.lite")) {
                            match = true
                        } else if (parentApp.packageName == "com.twitter.android" && a.packageName == "com.twitter.android.lite") {
                            match = true
                        }
                        if (match) {
                            matched.add(a)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    packageEntities = matched
                }
            }
        }
    }

    ComposeDialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = themeColor(R.attr.surface_card, Color.White)
            ),
            border = BorderStroke(1.dp, themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppIconImage(packageName = parentApp.packageName)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = when (parentApp.packageName) {
                                "com.google.android.youtube" -> "YouTube Variants"
                                "com.instagram.android" -> "Instagram Variants"
                                "com.snapchat.android" -> "Snapchat Variants"
                                "com.facebook.katana" -> "Facebook Variants"
                                "com.linkedin.android" -> "LinkedIn Variants"
                                "com.reddit.frontpage" -> "Reddit Variants"
                                "com.ss.android.ugc.trill" -> "TikTok Variants"
                                "com.twitter.android" -> "Twitter Variants"
                                else -> "${parentApp.appName} Variants"
                            },
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "Supported original, mod, and lite clients",
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            color = textTertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(packageEntities.size) { index ->
                        val item = packageEntities[index]
                        val isInstalled = remember(item.packageName) {
                            try {
                                context.packageManager.getPackageInfo(item.packageName, 0)
                                true
                            } catch (e: Exception) {
                                false
                            }
                        }

                        val category = remember(item.packageName) {
                            when {
                                item.packageName == parentApp.packageName -> "Original"
                                item.packageName.contains(".lite") || item.packageName.endsWith("lite") -> "Lite"
                                else -> "Mod"
                            }
                        }

                        PackageItemRow(
                            item = item,
                            category = category,
                            isInstalled = isInstalled,
                            InterFamily = InterFamily,
                            PoppinsFamily = PoppinsFamily,
                            textPrimary = textPrimary,
                            textTertiary = textTertiary,
                            brandPink = brandPink
                        )

                        if (index < packageEntities.lastIndex) {
                            HorizontalDivider(
                                color = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = brandPink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Close",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScopeControlSection(
    app: BlockedAppEntity,
    blockedAppDao: BlockedAppDao?,
    notifyService: () -> Unit,
    PoppinsFamily: FontFamily,
    InterFamily: FontFamily,
    textPrimary: Color,
    textTertiary: Color,
    brandPink: Color,
    onScopeUpdated: (BlockedAppEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scopeSetting by remember(app.packageName, app.scope) { mutableStateOf(app.scope ?: "section") }

    fun updateScopeInDb(newScope: String) {
        app.scope = newScope
        scopeSetting = newScope
        onScopeUpdated(app)

        scope.launch(Dispatchers.IO) {
            blockedAppDao?.update(app)

            blockedAppDao?.let { dao ->
                val allApps = dao.getAllSync()
                for (a in allApps) {
                    if (isModPackage(a.packageName)) {
                        var match = false
                        when (app.packageName) {
                            "com.google.android.youtube" -> if (a.packageName.contains("youtube")) match = true
                            "com.instagram.android" -> if (a.packageName.contains("insta") || a.packageName.contains("honista")) match = true
                            "com.facebook.katana" -> if (a.packageName == "com.facebook.lite") match = true
                            "com.ss.android.ugc.trill" -> if (a.packageName == "com.zhiliaoapp.musically" || a.packageName == "com.ss.android.ugc.aweme" || a.packageName == "com.ss.android.ugc.aweme.lite") match = true
                            "com.reddit.frontpage" -> if (a.packageName == "com.andrewshu.android.reddit" || a.packageName == "ml.docilealligator.infinityforreddit" || a.packageName == "free.reddit.news" || a.packageName == "com.laurencedawson.reddit_sync" || a.packageName == "com.reddit.frontpage.lite") match = true
                            "com.twitter.android" -> if (a.packageName == "com.twitter.android.lite") match = true
                        }
                        if (match) {
                            a.scope = newScope
                            dao.update(a)
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                notifyService()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(themeColor(R.attr.surface_nested, Color(0xFFF8FAFC)))
            .padding(14.dp)
    ) {
        Text(
            text = "Block Scope — ${app.appName ?: ""}",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val options = listOf("section", "full")
        val selectedIndex = options.indexOf(scopeSetting).coerceAtLeast(0)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)))
                .padding(2.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val indicatorWidth = maxWidth / 2
                val animatedOffset by animateDpAsState(
                    targetValue = if (selectedIndex == 0) 0.dp else indicatorWidth,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                )
                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset)
                        .width(indicatorWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(brandPink)
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (scopeSetting != "section") {
                                    val settingsLockMgr = SettingsLockManager(context)
                                    val activityContext = context as? FragmentActivity
                                    if (settingsLockMgr.isLockEnabled && activityContext != null) {
                                        settingsLockMgr.authenticate(activityContext, "Change Blocking Scope", object : SettingsLockManager.AuthCallback {
                                            override fun onSuccess() {
                                                updateScopeInDb("section")
                                            }
                                            override fun onFailure(reason: String?) {}
                                        })
                                    } else {
                                        updateScopeInDb("section")
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Doom-scrolling",
                            fontFamily = PoppinsFamily,
                            fontSize = 10.sp,
                            color = if (scopeSetting == "section") Color.White else textTertiary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (scopeSetting == "section") {
                                    updateScopeInDb("full")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Full App",
                            fontFamily = PoppinsFamily,
                            fontSize = 10.sp,
                            color = if (scopeSetting != "section") Color.White else textTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RestrictedAppsGridSection(
    appsList: List<BlockedAppEntity>,
    blockedAppDao: BlockedAppDao?,
    notifyService: () -> Unit,
    PoppinsFamily: FontFamily,
    InterFamily: FontFamily,
    textPrimary: Color,
    textTertiary: Color,
    brandPink: Color,
    settingsLockMgr: SettingsLockManager,
    activityContext: FragmentActivity?,
    onAppUpdated: (BlockedAppEntity) -> Unit
) {
    var longPressedApp by remember { mutableStateOf<BlockedAppEntity?>(null) }
    var expandedAppPackage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "RESTRICTED APPS",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = textTertiary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            letterSpacing = 1.sp
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = themeColor(R.attr.surface_card, Color.White)
            ),
            border = BorderStroke(1.dp, themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val chunks = appsList.chunked(4)
                chunks.forEachIndexed { rowIndex, rowApps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowApps.forEach { app ->
                            val isExpanded = expandedAppPackage == app.packageName
                            AppGridItem(
                                app = app,
                                PoppinsFamily = PoppinsFamily,
                                InterFamily = InterFamily,
                                textPrimary = textPrimary,
                                textTertiary = textTertiary,
                                brandPink = brandPink,
                                isExpanded = isExpanded,
                                onExpandClick = {
                                    expandedAppPackage = if (isExpanded) null else app.packageName
                                },
                                onClick = {
                                    handleAppClick(
                                        app = app,
                                        blockedAppDao = blockedAppDao,
                                        settingsLockMgr = settingsLockMgr,
                                        activityContext = activityContext,
                                        scope = scope,
                                        notifyService = notifyService,
                                        updateState = { r, m ->
                                            val updated = BlockedAppEntity().apply {
                                                packageName = app.packageName
                                                appName = app.appName
                                                isRestricted = r
                                                useMod = m
                                                this.scope = app.scope
                                                sectionViewId = app.sectionViewId
                                            }
                                            if (!r && isExpanded) {
                                                expandedAppPackage = null
                                            }
                                            onAppUpdated(updated)
                                        }
                                    )
                                },
                                onLongClick = {
                                    longPressedApp = app
                                }
                            )
                        }
                    }

                    val expandedAppInRow = rowApps.find { it.packageName == expandedAppPackage }
                    AnimatedVisibility(
                        visible = expandedAppInRow != null && expandedAppInRow.isRestricted,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        expandedAppInRow?.let { app ->
                            ScopeControlSection(
                                app = app,
                                blockedAppDao = blockedAppDao,
                                notifyService = notifyService,
                                PoppinsFamily = PoppinsFamily,
                                InterFamily = InterFamily,
                                textPrimary = textPrimary,
                                textTertiary = textTertiary,
                                brandPink = brandPink,
                                onScopeUpdated = { updatedApp ->
                                    onAppUpdated(updatedApp)
                                }
                            )
                        }
                    }

                    if (rowIndex < chunks.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    color = themeColor(R.attr.glass_stroke, Color(0xFFE2E8F0)),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.info),
                        contentDescription = "Info",
                        tint = textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Note: Now toggling an app blocks both the parent app and all its mods. Tap the right arrow on any active app to change scope between Doom-scrolling and Full App.",
                        fontFamily = InterFamily,
                        fontSize = 10.5.sp,
                        color = textTertiary,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }

    longPressedApp?.let { app ->
        PackageDetailDialog(
            parentApp = app,
            blockedAppDao = blockedAppDao,
            PoppinsFamily = PoppinsFamily,
            InterFamily = InterFamily,
            textPrimary = textPrimary,
            textTertiary = textTertiary,
            brandPink = brandPink,
            onDismiss = { longPressedApp = null }
        )
    }
}

fun handleAppClick(
    app: BlockedAppEntity,
    blockedAppDao: BlockedAppDao?,
    settingsLockMgr: SettingsLockManager,
    activityContext: FragmentActivity?,
    scope: kotlinx.coroutines.CoroutineScope,
    notifyService: () -> Unit,
    updateState: (Boolean, Boolean) -> Unit
) {
    val context = activityContext ?: return
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    val currentRestricted = app.isRestricted
    val nextRestricted = !currentRestricted
    val nextUseMod = nextRestricted
    val isTurningOff = currentRestricted

    fun performUpdate(restricted: Boolean, useMod: Boolean) {
        app.isRestricted = restricted
        app.useMod = useMod
        if (restricted) {
            app.scope = "section"
        }

        sharedPrefs.edit {
            val key = when (app.packageName) {
                "com.google.android.youtube" -> "ytSwitchState"
                "com.instagram.android" -> "instaSwitchState"
                "com.snapchat.android" -> "snapSwitchState"
                "com.facebook.katana" -> "facebookSwitchState"
                "com.linkedin.android" -> "linkedinSwitchState"
                "com.reddit.frontpage" -> "redditSwitchState"
                "com.ss.android.ugc.trill" -> "tiktokSwitchState"
                "com.twitter.android" -> "twitterSwitchState"
                else -> null
            }
            if (key != null) {
                putBoolean(key, restricted)
            }
        }

        scope.launch(Dispatchers.IO) {
            blockedAppDao?.update(app)

            blockedAppDao?.let { dao ->
                val allApps = dao.getAllSync()
                for (a in allApps) {
                    if (isModPackage(a.packageName)) {
                        var match = false
                        if (app.packageName == "com.google.android.youtube" && a.packageName.contains("youtube")) {
                            match = true
                        } else if (app.packageName == "com.instagram.android" && (a.packageName.contains("insta") || a.packageName.contains("honista"))) {
                            match = true
                        } else if (app.packageName == "com.facebook.katana" && a.packageName == "com.facebook.lite") {
                            match = true
                        } else if (app.packageName == "com.ss.android.ugc.trill" &&
                            (a.packageName == "com.zhiliaoapp.musically" ||
                             a.packageName == "com.ss.android.ugc.aweme" ||
                             a.packageName == "com.ss.android.ugc.aweme.lite")) {
                            match = true
                        } else if (app.packageName == "com.reddit.frontpage" &&
                            (a.packageName == "com.andrewshu.android.reddit" ||
                             a.packageName == "ml.docilealligator.infinityforreddit" ||
                             a.packageName == "free.reddit.news" ||
                             a.packageName == "com.laurencedawson.reddit_sync" ||
                             a.packageName == "com.reddit.frontpage.lite")) {
                            match = true
                        } else if (app.packageName == "com.twitter.android" && a.packageName == "com.twitter.android.lite") {
                            match = true
                        }
                        if (match) {
                            a.isRestricted = restricted && useMod
                            a.scope = app.scope
                            a.useMod = useMod
                            dao.update(a)
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                updateState(restricted, useMod)
                notifyService()
            }
        }
    }

    if (isTurningOff && settingsLockMgr.isLockEnabled) {
        settingsLockMgr.authenticate(context, "Disable App Restriction", object : SettingsLockManager.AuthCallback {
            override fun onSuccess() {
                performUpdate(nextRestricted, nextUseMod)
            }
            override fun onFailure(reason: String?) {}
        })
    } else {
        performUpdate(nextRestricted, nextUseMod)
    }
}
