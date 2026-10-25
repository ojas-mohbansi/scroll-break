package com.cascade26.scrollbreak.Activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.cascade26.scrollbreak.Utils.Utils.isAccessibilityPermissionGranted;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.cascade26.scrollbreak.Views.CustomPieChart;
import com.cascade26.scrollbreak.Fragments.StatsFocusFragment;
import com.cascade26.scrollbreak.Fragments.StatsHabitsFragment;
import com.cascade26.scrollbreak.Fragments.StatsTasksFragment;
import com.cascade26.scrollbreak.R;
import com.cascade26.scrollbreak.Utils.Utils;
import com.cascade26.scrollbreak.Models.StatsViewModel;

import java.util.ArrayList;
import java.util.List;

public class StatsActivity extends AppCompatActivity {

    private StatsViewModel viewModel;
    private ImageView brain;
    private ActivityResultLauncher<Intent> accessibilityLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        Utils.setPad(findViewById(R.id.main), "bottom", this);

        viewModel = new ViewModelProvider(this).get(StatsViewModel.class);

        setupScrollStats();
        setupNavigation();
        setupViewPager();
        View analyticsButton = findViewById(R.id.btnInterventionAnalytics);
        if (analyticsButton != null) {
            analyticsButton.setOnClickListener(v -> startActivity(new Intent(this, AnalyticsActivity.class)));
        }
        checkAndShowPermissionCard();
        registerForPermission();

        // Observe period label changes
        viewModel.getPeriodLabel().observe(this, label -> {
            TextView tv = findViewById(R.id.periodLabel);
            if (tv != null)
                tv.setText(label);
        });

        // Observe Mode
        viewModel.getIsWeekly().observe(this, this::updateWeeklyMonthlyTabs);
    }

    private void setupViewPager() {
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        StatsPagerAdapter adapter = new StatsPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("HABITS");
                    break;
                case 1:
                    tab.setText("FOCUS");
                    break;
                case 2:
                    tab.setText("TASKS");
                    break;
            }
        }).attach();
    }

    // Adapter
    private static class StatsPagerAdapter extends FragmentStateAdapter {

        public StatsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case 1 -> new StatsFocusFragment();
                case 2 -> new StatsTasksFragment();
                default -> new StatsHabitsFragment();
            };
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }

    private void setupScrollStats() {
        CustomPieChart pieChart = findViewById(R.id.pieChartX);
        brain = findViewById(R.id.brainImage);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int instaScrolls = Utils.calculateTotalUsageScrolls(sharedPreferences, "insta");
        int ytScrolls = Utils.calculateTotalUsageScrolls(sharedPreferences, "yt");
        int snapScrolls = Utils.calculateTotalUsageScrolls(sharedPreferences, "snap");

        List<Integer> data = new ArrayList<>();
        List<String> segmentNames = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        if (instaScrolls > 0) {
            data.add(instaScrolls);
            segmentNames.add("Instagram");
            colors.add(ContextCompat.getColor(this, R.color.sexyInsta));
        }
        if (ytScrolls > 0) {
            data.add(ytScrolls);
            segmentNames.add("YouTube");
            colors.add(ContextCompat.getColor(this, R.color.sexyYt));
        }
        if (snapScrolls > 0) {
            data.add(snapScrolls);
            segmentNames.add("Snapchat");
            colors.add(ContextCompat.getColor(this, R.color.sexySnap));
        }

        // Convert list to array
        int[] pieChartColors = new int[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            pieChartColors[i] = colors.get(i);
        }

        pieChart.setData(data, segmentNames, pieChartColors);
        int totalScrolls = ytScrolls + instaScrolls + snapScrolls;
        updateBrainImage(totalScrolls);

        TextView tvTotalScrolls = findViewById(R.id.tvTotalScrolls);
        tvTotalScrolls.setText("Total Scrolls: " + totalScrolls);

        setupStatRows(instaScrolls, ytScrolls, snapScrolls);
    }

    private void setupNavigation() {
        TextView btnWeekly = findViewById(R.id.btnWeekly);
        TextView btnMonthly = findViewById(R.id.btnMonthly);

        btnWeekly.setOnClickListener(v -> viewModel.setWeekly(true));
        btnMonthly.setOnClickListener(v -> viewModel.setWeekly(false));

        findViewById(R.id.arrowLeft).setOnClickListener(v -> viewModel.decrementOffset());
        findViewById(R.id.arrowRight).setOnClickListener(v -> viewModel.incrementOffset());
    }

    private void updateWeeklyMonthlyTabs(boolean isWeekly) {
        TextView btnWeekly = findViewById(R.id.btnWeekly);
        TextView btnMonthly = findViewById(R.id.btnMonthly);

        if (isWeekly) {
            btnWeekly.setBackgroundResource(R.drawable.bg_segment_selected);
            btnWeekly.setTextColor(getAttrColor(R.attr.text_primary));
            btnMonthly.setBackgroundResource(0);
            btnMonthly.setTextColor(getAttrColor(R.attr.text_tertiary));
        } else {
            btnMonthly.setBackgroundResource(R.drawable.bg_segment_selected);
            btnMonthly.setTextColor(getAttrColor(R.attr.text_primary));
            btnWeekly.setBackgroundResource(0);
            btnWeekly.setTextColor(getAttrColor(R.attr.text_tertiary));
        }
    }

    private void setupStatRows(int instaScrolls, int ytScrolls, int snapScrolls) {
        View statInstaView = findViewById(R.id.statInsta);
        ImageView instaIcon = statInstaView.findViewById(R.id.ivAppIcon);
        TextView instaName = statInstaView.findViewById(R.id.tvAppName);
        TextView instaValue = statInstaView.findViewById(R.id.tvAppValue);

        instaIcon.setImageResource(R.drawable.instagram);
        instaIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.sexyInsta)));
        instaName.setText("Insta");
        instaValue.setText(String.valueOf(instaScrolls));

        View statYoutubeView = findViewById(R.id.statYoutube);
        ImageView ytIcon = statYoutubeView.findViewById(R.id.ivAppIcon);
        TextView ytName = statYoutubeView.findViewById(R.id.tvAppName);
        TextView ytValue = statYoutubeView.findViewById(R.id.tvAppValue);

        ytIcon.setImageResource(R.drawable.youtube);
        ytIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.sexyYt)));
        ytName.setText("YT");
        ytValue.setText(String.valueOf(ytScrolls));

        View statSnapView = findViewById(R.id.statSnap);
        ImageView snapIcon = statSnapView.findViewById(R.id.ivAppIcon);
        TextView snapName = statSnapView.findViewById(R.id.tvAppName);
        TextView snapValue = statSnapView.findViewById(R.id.tvAppValue);

        snapIcon.setImageResource(R.drawable.snapchat);
        snapIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.sexySnap)));
        snapName.setText("Snap");
        snapValue.setText(String.valueOf(snapScrolls));
    }

    private void updateBrainImage(int totalWastedScrolls) {
        int drawableId;
        if (totalWastedScrolls < 150)
            drawableId = R.drawable.brain1;
        else if (totalWastedScrolls < 300)
            drawableId = R.drawable.brain2;
        else if (totalWastedScrolls < 500)
            drawableId = R.drawable.brain3;
        else if (totalWastedScrolls < 700)
            drawableId = R.drawable.brain4;
        else if (totalWastedScrolls < 900)
            drawableId = R.drawable.brain5;
        else if (totalWastedScrolls < 1100)
            drawableId = R.drawable.brain6;
        else if (totalWastedScrolls < 1200)
            drawableId = R.drawable.brain7;
        else if (totalWastedScrolls < 1400)
            drawableId = R.drawable.brain8;
        else
            drawableId = R.drawable.brain9;
        brain.setImageResource(drawableId);
    }

    private void checkAndShowPermissionCard() {
        ConstraintLayout permissionCard = findViewById(R.id.permissionCard);
        if (!isAccessibilityPermissionGranted(this)) {
            permissionCard.setVisibility(VISIBLE);
            permissionCard.setOnClickListener(v -> reqAccess());
        } else {
            permissionCard.setVisibility(GONE);
        }
    }

    /** Shared stats setup for the Home pager and the legacy widget-launchable activity. */
    public static void setupEmbeddedStats(androidx.fragment.app.Fragment fragment, View root) {
        // The report tabs are child fragments and observe the host activity's model.
        // Share it here so period selection and offset changes update those tabs too.
        StatsViewModel model = new ViewModelProvider(fragment.requireActivity()).get(StatsViewModel.class);
        TextView weekly = root.findViewById(R.id.btnWeekly);
        TextView monthly = root.findViewById(R.id.btnMonthly);
        TextView period = root.findViewById(R.id.periodLabel);
        if (weekly != null) weekly.setOnClickListener(v -> model.setWeekly(true));
        if (monthly != null) monthly.setOnClickListener(v -> model.setWeekly(false));
        View left = root.findViewById(R.id.arrowLeft);
        View right = root.findViewById(R.id.arrowRight);
        if (left != null) left.setOnClickListener(v -> model.decrementOffset());
        if (right != null) right.setOnClickListener(v -> model.incrementOffset());
        model.getPeriodLabel().observe(fragment.getViewLifecycleOwner(), label -> {
            if (period != null) period.setText(label);
        });
        model.getIsWeekly().observe(fragment.getViewLifecycleOwner(), isWeekly -> {
            if (weekly == null || monthly == null) return;
            if (isWeekly) {
                weekly.setBackgroundResource(R.drawable.bg_segment_selected);
                weekly.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.text_primary));
                monthly.setBackgroundResource(0);
                monthly.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.text_tertiary));
            } else {
                monthly.setBackgroundResource(R.drawable.bg_segment_selected);
                monthly.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.text_primary));
                weekly.setBackgroundResource(0);
                weekly.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.text_tertiary));
            }
        });
        CustomPieChart chart = root.findViewById(R.id.pieChartX);
        if (chart != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(fragment.requireContext());
            int instagram = Utils.calculateTotalUsageScrolls(prefs, "insta");
            int youtube = Utils.calculateTotalUsageScrolls(prefs, "yt");
            int snapchat = Utils.calculateTotalUsageScrolls(prefs, "snap");
            List<Integer> values = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            if (instagram > 0) { values.add(instagram); names.add("Instagram"); colors.add(ContextCompat.getColor(fragment.requireContext(), R.color.sexyInsta)); }
            if (youtube > 0) { values.add(youtube); names.add("YouTube"); colors.add(ContextCompat.getColor(fragment.requireContext(), R.color.sexyYt)); }
            if (snapchat > 0) { values.add(snapchat); names.add("Snapchat"); colors.add(ContextCompat.getColor(fragment.requireContext(), R.color.sexySnap)); }
            int[] series = new int[colors.size()];
            for (int i = 0; i < series.length; i++) series[i] = colors.get(i);
            chart.setData(values, names, series);
            int total = instagram + youtube + snapchat;
            TextView totalView = root.findViewById(R.id.tvTotalScrolls);
            if (totalView != null) totalView.setText("Total Scrolls: " + total);
            ImageView brainImage = root.findViewById(R.id.brainImage);
            if (brainImage != null) {
                int image = total < 150 ? R.drawable.brain1 : total < 300 ? R.drawable.brain2 : total < 500 ? R.drawable.brain3
                        : total < 700 ? R.drawable.brain4 : total < 900 ? R.drawable.brain5 : total < 1100 ? R.drawable.brain6
                        : total < 1200 ? R.drawable.brain7 : total < 1400 ? R.drawable.brain8 : R.drawable.brain9;
                brainImage.setImageResource(image);
            }
            bindEmbeddedStatRow(root, R.id.statInsta, R.drawable.instagram, R.color.sexyInsta, "Insta", instagram, fragment);
            bindEmbeddedStatRow(root, R.id.statYoutube, R.drawable.youtube, R.color.sexyYt, "YT", youtube, fragment);
            bindEmbeddedStatRow(root, R.id.statSnap, R.drawable.snapchat, R.color.sexySnap, "Snap", snapchat, fragment);
        }
        View permission = root.findViewById(R.id.permissionCard);
        if (permission != null) {
            boolean missing = !isAccessibilityPermissionGranted(fragment.requireContext());
            permission.setVisibility(missing ? VISIBLE : GONE);
            permission.setOnClickListener(v -> Utils.showPermissionSheet(fragment.requireContext(), Utils.PermissionType.ACCESSIBILITY,
                    new Utils.PermissionLauncher() {
                        @Override public void launchAccessibility(Intent intent) { fragment.startActivity(intent); }
                        @Override public void launchBattery(Intent intent) { }
                        @Override public void launchNotification(String permission) { }
                    }, () -> Toast.makeText(fragment.requireContext(), "Accessibility permission not granted.", Toast.LENGTH_SHORT).show()));
        }
    }

    private static void bindEmbeddedStatRow(View root, int rowId, int iconId, int tintId, String name, int count, Fragment fragment) {
        View row = root.findViewById(rowId);
        if (row == null) return;
        ImageView icon = row.findViewById(R.id.ivAppIcon);
        TextView title = row.findViewById(R.id.tvAppName);
        TextView value = row.findViewById(R.id.tvAppValue);
        icon.setImageResource(iconId);
        icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), tintId)));
        title.setText(name);
        value.setText(String.valueOf(count));
    }

    private void reqAccess() {
        Utils.showPermissionSheet(this, Utils.PermissionType.ACCESSIBILITY,
                new Utils.PermissionLauncher() {
                    @Override
                    public void launchAccessibility(Intent intent) {
                        if (accessibilityLauncher != null) {
                            accessibilityLauncher.launch(intent);
                        } else {
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void launchBattery(Intent intent) {
                    }

                    @Override
                    public void launchNotification(String permission) {
                    }
                },
                () -> {
                    Toast.makeText(this, "Accessibility permission not granted.", Toast.LENGTH_SHORT).show();
                    checkAndShowPermissionCard();
                });
    }

    public void registerForPermission() {
        accessibilityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (isAccessibilityPermissionGranted(this)) {
                        Toast.makeText(this, "Thank you for granting Accessibility permission!", Toast.LENGTH_SHORT)
                                .show();
                        checkAndShowPermissionCard();
                    } else {
                        Toast.makeText(this, "Accessibility permission not granted.", Toast.LENGTH_SHORT).show();
                        checkAndShowPermissionCard();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndShowPermissionCard();
    }

    private int getAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
