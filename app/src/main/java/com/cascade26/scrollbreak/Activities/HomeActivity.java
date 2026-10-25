package com.cascade26.scrollbreak.Activities;

import android.Manifest;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.cascade26.scrollbreak.Adapters.HomePagerAdapter;
import com.cascade26.scrollbreak.Common.IntentActions;
import com.cascade26.scrollbreak.Fragments.HabitFragment;
import com.cascade26.scrollbreak.Fragments.HomeFragment;
import com.cascade26.scrollbreak.Fragments.SettingsFragment;
import com.cascade26.scrollbreak.Fragments.StatsFragment;
import com.cascade26.scrollbreak.Fragments.ShopFragment;
import com.cascade26.scrollbreak.R;
import com.cascade26.scrollbreak.Utils.MidnightResetManager;
import com.cascade26.scrollbreak.Utils.Utils;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.cascade26.scrollbreak.Services.FocusService;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;
import com.skydoves.balloon.BalloonSizeSpec;


public class HomeActivity extends AppCompatActivity {

    public static final String EXTRA_START_FRAGMENT = "extra_start_fragment";
    public static final String START_FRAGMENT_HOME = "home";
    public static final String START_FRAGMENT_TASKS = "tasks";
    public static final String START_FRAGMENT_HABITS = "habits";
    public static final String START_FRAGMENT_SHOP = "shop";
    public static final String START_FRAGMENT_SETTINGS = "settings";
    public static final String START_FRAGMENT_STATS = "stats";
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String PREF_START_FRAGMENT = "pref_start_fragment";

    private static final String PREF_LOCK_IN_SAVED_MS = "pref_lock_in_saved_ms";
    private static final String PREF_LOCK_IN_WARNING_SHOWN = "pref_lock_in_warning_shown";

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private android.widget.TextView pageTitle;
    private MaterialButton lockInPill;
    private android.content.BroadcastReceiver focusSessionEndedReceiver;

    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            syncNavToPage(position);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (root, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            root.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        findViews();
        setupAdapter();
        setupNavigation();
        setupBackPressHandler();

        if (savedInstanceState == null) routeToIntent(getIntent());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        routeToIntent(intent);
    }

    private void routeToIntent(Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String startFromIntent = intent != null ? intent.getStringExtra(EXTRA_START_FRAGMENT) : null;
        String startFromPrefs = prefs.getString(PREF_START_FRAGMENT, null);
        if (startFromPrefs != null) prefs.edit().remove(PREF_START_FRAGMENT).apply();
        String start = startFromIntent != null ? startFromIntent : startFromPrefs;

        boolean shouldOpenAddTask = intent != null && intent.getBooleanExtra("open_add_task", false);
        String taskId = intent != null ? intent.getStringExtra(EXTRA_TASK_ID) : null;
        boolean shouldOpenTaskManagement = START_FRAGMENT_TASKS.equals(start);
        if (shouldOpenAddTask || taskId != null || shouldOpenTaskManagement) start = START_FRAGMENT_HOME;
        if (start == null) start = START_FRAGMENT_HOME;

        viewPager.setCurrentItem(pageIndexFor(start), false);
        if (shouldOpenAddTask || taskId != null || shouldOpenTaskManagement) {
            viewPager.post(() -> {
                Fragment f = getSupportFragmentManager()
                        .findFragmentByTag("f" + HomePagerAdapter.PAGE_HOME);
                if (f instanceof HomeFragment) ((HomeFragment) f).openTaskManagementSheet(taskId, shouldOpenAddTask);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MidnightResetManager.checkAndPerformReset(this);
        if (viewPager != null) syncNavToPage(viewPager.getCurrentItem());
        updateLockInPillLabel();

        if (viewPager != null && viewPager.getCurrentItem() == HomePagerAdapter.PAGE_HOME) {
            lockInPill.postDelayed(this::checkAndShowLockInTutorial, 500);
        }

        focusSessionEndedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateLockInPillLabel();
                Fragment f = getSupportFragmentManager()
                        .findFragmentByTag("f" + HomePagerAdapter.PAGE_HOME);
                if (f instanceof HomeFragment) {
                    ((HomeFragment) f).refreshPauseButtonState();
                }
            }
        };
        ContextCompat.registerReceiver(this, focusSessionEndedReceiver,
                new IntentFilter(IntentActions.getActionFocusSessionEnded(this)), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (focusSessionEndedReceiver != null) {
            try {
                unregisterReceiver(focusSessionEndedReceiver);
            } catch (Exception ignored) {
            }
            focusSessionEndedReceiver = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
    }

    private void findViews() {
        viewPager = findViewById(R.id.nav_host_container);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        pageTitle = findViewById(R.id.pageTitle);

        lockInPill = findViewById(R.id.lockInPill);
        updateLockInPillLabel();
    }

    private void setupAdapter() {
        HomePagerAdapter pagerAdapter = new HomePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        viewPager.setOffscreenPageLimit(3);

        viewPager.setOverScrollMode(ViewPager2.OVER_SCROLL_NEVER);

        viewPager.registerOnPageChangeCallback(pageChangeCallback);
    }

    private void setupNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            int page;
            if (id == R.id.menu_stats) page = HomePagerAdapter.PAGE_STATS;
            else if (id == R.id.menu_habits) page = HomePagerAdapter.PAGE_HABITS;
            else if (id == R.id.menu_shop) page = HomePagerAdapter.PAGE_SHOP;
            else if (id == R.id.menu_settings) page = HomePagerAdapter.PAGE_SETTINGS;
            else page = HomePagerAdapter.PAGE_HOME;
            if (viewPager.getCurrentItem() != page) viewPager.setCurrentItem(page, true);
            return true;
        });

        lockInPill.setOnClickListener(v -> {
            // If focus/lock-in session is already active, open FocusMode to show timer
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isAlreadyActive = FocusService.isPublicFocusRun
                    || sharedPreferences.getBoolean(FocusService.PREF_IS_LOCKED_IN, false);
            if (isAlreadyActive) {
                startActivity(new Intent(this, FocusMode.class));
                return;
            }
            // Require notification + exact alarm permissions before starting
            if (!hasLockInPermissions()) {
                // Open FocusMode and request it to shake the missing-permission cards
                Intent permIntent = new Intent(this, FocusMode.class);
                permIntent.putExtra(FocusMode.EXTRA_SHAKE_PERM_CARDS, true);
                startActivity(permIntent);
                return;
            }
            long savedMs = sharedPreferences.getLong(PREF_LOCK_IN_SAVED_MS, 0);
            if (savedMs > 0) {
                // One-tap lock: start immediately with saved time
                startLockInMode(savedMs);
            } else {
                showLockInSheet();
            }
        });

        lockInPill.setOnLongClickListener(v -> {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isAlreadyActive = FocusService.isPublicFocusRun
                    || sharedPreferences.getBoolean(FocusService.PREF_IS_LOCKED_IN, false);
            if (isAlreadyActive) {
                startActivity(new Intent(this, FocusMode.class));
                return true;
            }
            showLockInSheet();
            return true;
        });

        viewPager.post(() -> syncNavToPage(viewPager.getCurrentItem()));
    }

    private void showLockInSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_time, null);

        // Customize title
        TextView titleLabel = sheetView.findViewById(R.id.greetings);
        if (titleLabel != null)
            titleLabel.setText("Lock In");
        TextView subtitle = sheetView.findViewById(R.id.dateDetails);
        if (subtitle != null)
            subtitle.setText("SET DURATION");

        NumberPicker hourPicker = sheetView.findViewById(R.id.hours_selector_bottom_sheet);
        NumberPicker minutePicker = sheetView.findViewById(R.id.minutes_selector_bottom_sheet);
        Button confirmBtn = sheetView.findViewById(R.id.setLimitBtnBottomSheet);
        View rememberRow = sheetView.findViewById(R.id.rememberTimeRow);
        MaterialSwitch rememberSwitch = sheetView.findViewById(R.id.rememberTimeSwitch);

        if (confirmBtn != null)
            confirmBtn.setText("Lock In");
        if (rememberRow != null)
            rememberRow.setVisibility(View.VISIBLE);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        long savedMs = sharedPreferences.getLong(PREF_LOCK_IN_SAVED_MS, 0);
        int savedHours = (int) (savedMs / 3600000);
        int savedMinutes = (int) ((savedMs % 3600000) / 60000);

        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(3);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        hourPicker.setValue(savedHours);
        minutePicker.setValue(savedMinutes > 0 ? savedMinutes : 25);

        if (rememberSwitch != null && savedMs > 0) {
            rememberSwitch.setChecked(true);
        }

        if (confirmBtn != null) {
            confirmBtn.setOnClickListener(v -> {
                int hours = hourPicker.getValue();
                int minutes = minutePicker.getValue();
                long durationMs = (hours * 3600L + minutes * 60L) * 1000L;
                if (durationMs <= 0) {
                    Toast.makeText(this, "Pick a duration", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (rememberSwitch != null && rememberSwitch.isChecked()) {
                    sharedPreferences.edit().putLong(PREF_LOCK_IN_SAVED_MS, durationMs).apply();
                } else {
                    sharedPreferences.edit().remove(PREF_LOCK_IN_SAVED_MS).apply();
                }
                startLockInMode(durationMs);
                updateLockInPillLabel();
                sheet.dismiss();
            });
        }

        sheetView.findViewById(R.id.crossBtn).setOnClickListener(v -> sheet.dismiss());

        sheet.setContentView(sheetView);
        sheet.show();
    }


    private boolean hasLockInPermissions() {
        // Exact alarm — required on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am == null || !am.canScheduleExactAlarms()) return false;
        }
        // Notification — required on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void startLockInMode(long durationMs) {
        // Guard: do not start a second focus session if one is already live
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (FocusService.isPublicFocusRun || sp.getBoolean(FocusService.PREF_IS_LOCKED_IN, false)) {
            startActivity(new Intent(this, FocusMode.class));
            return;
        }

        // First-time warning: user must explicitly accept that they cannot stop early
        if (!sp.getBoolean(PREF_LOCK_IN_WARNING_SHOWN, false)) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("🔒 Lock In Mode")
                    .setMessage(
                            """
                                    Once you start Lock In, all non-essential apps will be blocked \
                                    for the entire duration you set.
                                    
                                    You will NOT be able to stop the session early. \
                                    Make sure you are ready before confirming.
                                    
                                    Essential apps (calls, camera, settings) remain accessible.""")
                    .setPositiveButton("I'm Ready, Lock In", (d, w) -> {
                        sp.edit().putBoolean(PREF_LOCK_IN_WARNING_SHOWN, true).apply();
                        doStartLockIn(durationMs);
                    })
                    .setNegativeButton("Cancel", null)
                    .setCancelable(true)
                    .show();
        } else {
            doStartLockIn(durationMs);
        }
    }

    private void doStartLockIn(long durationMs) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        // Clear any stale pause state that could interfere with the new session
        sp.edit()
                .putBoolean("isServicePaused", false)
                .putLong("resumeTime", 0)
                .apply();

        Intent serviceIntent = new Intent(this, FocusService.class);
        serviceIntent.setAction(FocusService.ACTION_START_FOREGROUND_SERVICE);
        serviceIntent.putExtra("durationInMillis", durationMs);
        serviceIntent.putExtra(FocusService.EXTRA_IS_LOCKED_IN, true);
        serviceIntent.putExtra("topicName", "Locked In");
        // startForegroundService ensures the service starts reliably on Android 8+
        androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);

        // Open FocusMode activity for visual feedback
        startActivity(new Intent(this, FocusMode.class));
        Toast.makeText(this, "Locked In!", Toast.LENGTH_SHORT).show();
    }

    public void updateLockInPillLabel() {
        if (lockInPill == null)
            return;

        boolean isHome = viewPager != null && viewPager.getCurrentItem() == HomePagerAdapter.PAGE_HOME;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        boolean isFocusActive = FocusService.isPublicFocusRun
                || sharedPreferences.getBoolean(FocusService.PREF_IS_LOCKED_IN, false);

        ConstraintLayout.LayoutParams pillParams = (ConstraintLayout.LayoutParams) lockInPill.getLayoutParams();
        pillParams.bottomToTop = isHome ? R.id.bottomNavDock : ConstraintLayout.LayoutParams.PARENT_ID;
        pillParams.bottomMargin = (int) ((isHome ? 12 : 20) * getResources().getDisplayMetrics().density);
        lockInPill.setLayoutParams(pillParams);
        if (!Utils.isAccessibilityPermissionGranted(this) || !isHome || isFocusActive) {
            lockInPill.setVisibility(View.GONE);
            return;
        } else {
            lockInPill.setVisibility(View.VISIBLE);
        }

        long savedMs = sharedPreferences.getLong(PREF_LOCK_IN_SAVED_MS, 0);
        if (savedMs > 0) {
            int totalMin = (int) (savedMs / 60000);
            int h = totalMin / 60;
            int m = totalMin % 60;
            String label = h > 0 ? "Lock In · " + h + "h" + (m > 0 ? m + "m" : "") : "Lock In · " + m + "m";
            lockInPill.setText(label);
        } else {
            lockInPill.setText("Lock In");
        }
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (viewPager.getCurrentItem() == HomePagerAdapter.PAGE_HOME) {
                    finish();
                } else {
                    viewPager.setCurrentItem(HomePagerAdapter.PAGE_HOME, true);
                }
            }
        });
    }

    private void syncNavToPage(int page) {
        int title = switch (page) {
            case HomePagerAdapter.PAGE_HABITS -> R.string.habits;
            case HomePagerAdapter.PAGE_SETTINGS -> R.string.settings;
            case HomePagerAdapter.PAGE_STATS -> R.string.stats;
            case HomePagerAdapter.PAGE_SHOP -> R.string.attention_shop_title;
            default -> R.string.home;
        };
        pageTitle.setText(title);
        int menuId = switch (page) {
            case HomePagerAdapter.PAGE_STATS -> R.id.menu_stats;
            case HomePagerAdapter.PAGE_HABITS -> R.id.menu_habits;
            case HomePagerAdapter.PAGE_SHOP -> R.id.menu_shop;
            case HomePagerAdapter.PAGE_SETTINGS -> R.id.menu_settings;
            default -> R.id.menu_home;
        };
        if (bottomNavigation.getSelectedItemId() != menuId) bottomNavigation.setSelectedItemId(menuId);
        updateLockInPillLabel();
    }

    private int pageForFragment(Fragment f) {
        if (f instanceof HabitFragment)
            return HomePagerAdapter.PAGE_HABITS;
        if (f instanceof SettingsFragment)
            return HomePagerAdapter.PAGE_SETTINGS;
        if (f instanceof StatsFragment)
            return HomePagerAdapter.PAGE_STATS;
        if (f instanceof ShopFragment)
            return HomePagerAdapter.PAGE_SHOP;
        return HomePagerAdapter.PAGE_HOME;
    }

    private int pageIndexFor(String startKey) {
        return switch (startKey) {
            case START_FRAGMENT_TASKS, START_FRAGMENT_HOME -> HomePagerAdapter.PAGE_HOME;
            case START_FRAGMENT_HABITS -> HomePagerAdapter.PAGE_HABITS;
            case START_FRAGMENT_SETTINGS -> HomePagerAdapter.PAGE_SETTINGS;
            case START_FRAGMENT_STATS -> HomePagerAdapter.PAGE_STATS;
            case START_FRAGMENT_SHOP -> HomePagerAdapter.PAGE_SHOP;
            default -> HomePagerAdapter.PAGE_HOME;
        };
    }


    private void checkAndShowLockInTutorial() {
        if (lockInPill == null || lockInPill.getVisibility() != View.VISIBLE)
            return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("lock_in_tutorial_shown", false))
            return;

        Balloon balloon = new Balloon.Builder(this)
                .setArrowSize(10)
                .setArrowOrientation(ArrowOrientation.BOTTOM)
                .setArrowPosition(0.5f)
                .setWidthRatio(0.7f)
                .setHeight(BalloonSizeSpec.WRAP)
                .setTextSize(14f)
                .setCornerRadius(10f)
                .setAlpha(0.95f)
                .setPadding(12)
                .setText("Tap to lock in and block distractive apps!")
                .setTextColor(ContextCompat.getColor(this, R.color.white))
                .setBackgroundColor(ContextCompat.getColor(this, R.color.brainColor))
                .setBalloonAnimation(BalloonAnimation.ELASTIC)
                .setDismissWhenClicked(true)
                .setLifecycleOwner(this)
                .setOnBalloonDismissListener(() -> prefs.edit().putBoolean("lock_in_tutorial_shown", true).apply())
                .build();
        balloon.showAlignTop(lockInPill);
    }
}
