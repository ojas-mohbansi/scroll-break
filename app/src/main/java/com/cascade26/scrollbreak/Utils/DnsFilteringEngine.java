package com.cascade26.scrollbreak.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.cascade26.scrollbreak.db.ScrollBreakRoomDatabase;
import com.cascade26.scrollbreak.db.dao.BlocklistDao;

public class DnsFilteringEngine {
    private static DnsFilteringEngine INSTANCE;
    private final BlocklistDao blocklistDao;
    private final SharedPreferences preferences;
    private final LruCache<String, Boolean> lruCache;

    private DnsFilteringEngine(@NonNull Context context) {
        ScrollBreakRoomDatabase db = ScrollBreakRoomDatabase.getInstance(context);
        this.blocklistDao = db.blocklistDao();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.lruCache = new LruCache<>(5000);
    }

    public static synchronized DnsFilteringEngine getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new DnsFilteringEngine(context.getApplicationContext());
        }
        return INSTANCE;
    }

    public boolean shouldBlock(@NonNull String domain) {
        String normalizedDomain = domain.trim().toLowerCase();

        // 1. Check LRU cache first (< 0.01ms)
        Boolean cachedVerdict = lruCache.get(normalizedDomain);
        if (cachedVerdict != null) {
            return cachedVerdict;
        }

        // 2. Query Room DB / SQLite index
        String category = blocklistDao.getCategoryForDomain(normalizedDomain);
        boolean blocked = false;

        if (category != null) {
            blocked = evaluateCategory(category);
        } else {
            // Check wildcard/parent domain suffix matching if needed
            blocked = checkParentDomain(normalizedDomain);
        }

        lruCache.put(normalizedDomain, blocked);
        return blocked;
    }

    private boolean evaluateCategory(@NonNull String category) {
        switch (category) {
            case "ad":
                return preferences.getBoolean("pref_block_ads", true);
            case "malware":
            case "scam":
                return true; // Always block security threats
            case "adult":
                return preferences.getBoolean("pref_block_adult", false);
            case "social":
                return preferences.getBoolean("pref_block_social", false);
            default:
                return false;
        }
    }

    private boolean checkParentDomain(String domain) {
        int idx = domain.indexOf('.');
        while (idx != -1 && idx < domain.length() - 1) {
            domain = domain.substring(idx + 1);
            String cat = blocklistDao.getCategoryForDomain(domain);
            if (cat != null) {
                return evaluateCategory(cat);
            }
            idx = domain.indexOf('.');
        }
        return false;
    }
}
