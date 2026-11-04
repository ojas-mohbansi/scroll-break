package com.cascade26.scrollbreak.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.cascade26.scrollbreak.Activities.StatsActivity;
import com.cascade26.scrollbreak.Activities.AnalyticsActivity;
import com.cascade26.scrollbreak.R;
import com.cascade26.scrollbreak.Models.StatsViewModel;
import androidx.lifecycle.ViewModelProvider;

/** Embeds the existing three-section stats pager as a primary HomeActivity destination. */
public class StatsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewPager2 pager = view.findViewById(R.id.viewPager);
        TabLayout tabs = view.findViewById(R.id.tabLayout);
        pager.setAdapter(new StatsPagerAdapter(this));
        new ViewModelProvider(requireActivity()).get(StatsViewModel.class);
        pager.setOffscreenPageLimit(3);
        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            switch (position) {
                case 1 -> tab.setText(R.string.focus_mode);
                case 2 -> tab.setText(R.string.tasks);
                default -> tab.setText(R.string.habits);
            }
        }).attach();
        StatsActivity.setupEmbeddedStats(this, view);
        View analyticsButton = view.findViewById(R.id.btnInterventionAnalytics);
        if (analyticsButton != null) {
            analyticsButton.setOnClickListener(v -> startActivity(
                    new android.content.Intent(requireContext(), AnalyticsActivity.class)));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            StatsActivity.setupEmbeddedStats(this, getView());
        }
    }

    private static class StatsPagerAdapter extends FragmentStateAdapter {
        StatsPagerAdapter(@NonNull Fragment fragment) { super(fragment); }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case 1 -> new StatsFocusFragment();
                case 2 -> new StatsTasksFragment();
                default -> new StatsHabitsFragment();
            };
        }

        @Override public int getItemCount() { return 3; }
    }
}
