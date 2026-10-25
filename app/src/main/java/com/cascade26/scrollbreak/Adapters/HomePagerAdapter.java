package com.cascade26.scrollbreak.Adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.cascade26.scrollbreak.Fragments.HabitFragment;
import com.cascade26.scrollbreak.Fragments.HomeFragment;
import com.cascade26.scrollbreak.Fragments.ShopFragment;
import com.cascade26.scrollbreak.Fragments.SettingsFragment;
import com.cascade26.scrollbreak.Fragments.StatsFragment;

public class HomePagerAdapter extends FragmentStateAdapter {

    // Keep this order aligned with the five-item bottom navigation.
    public static final int PAGE_STATS    = 0;
    public static final int PAGE_HABITS   = 1;
    public static final int PAGE_HOME     = 2;
    public static final int PAGE_SHOP     = 3;
    public static final int PAGE_SETTINGS = 4;
    public static final int PAGE_COUNT    = 5;

    public HomePagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return switch (position) {
            case PAGE_STATS -> new StatsFragment();
            case PAGE_HABITS -> new HabitFragment();
            case PAGE_HOME -> new HomeFragment();
            case PAGE_SHOP -> new ShopFragment();
            case PAGE_SETTINGS -> new SettingsFragment();
            default -> new HomeFragment();
        };
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
