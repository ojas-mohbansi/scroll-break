package com.cascade26.scrollbreak.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

/** Local Attention Currency balance with a one-time migration from the original preference keys. */
public class AttentionCurrency {
    public static final String PREFS_NAME = "AttentionCurrencyPrefs";
    public static final String COINS_KEY = "balance";
    private static final String LEGACY_PREFS_NAME = "PeaceCoinsPrefs";
    private static final String LEGACY_COINS_KEY = "PeaceCoins";

    private final SharedPreferences prefs;

    public AttentionCurrency(Context context) {
        Context appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        migrateLegacyBalance(appContext, prefs);
    }

    private static void migrateLegacyBalance(Context context, SharedPreferences currentPrefs) {
        if (currentPrefs.contains(COINS_KEY)) return;
        SharedPreferences legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE);
        if (legacyPrefs.contains(LEGACY_COINS_KEY)) {
            currentPrefs.edit().putInt(COINS_KEY, Math.max(0, legacyPrefs.getInt(LEGACY_COINS_KEY, 0))).apply();
        }
    }

    public synchronized void addBalance(int amount) {
        setBalance(getBalance() + Math.max(0, amount));
    }

    public synchronized void deductBalance(int amount) {
        setBalance(Math.max(0, getBalance() - Math.max(0, amount)));
    }

    public synchronized int getBalance() {
        return prefs.getInt(COINS_KEY, 0);
    }

    /** Writes the historical backup shape so backups made by older releases stay compatible. */
    public synchronized Map<String, ?> getLegacyBackupPrefs(Context context) {
        Map<String, Object> backupPrefs = new HashMap<>(
                context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE).getAll());
        backupPrefs.put(LEGACY_COINS_KEY, getBalance());
        return backupPrefs;
    }

    /** Restores the historical backup shape and syncs its balance into the current preference store. */
    public static void restoreLegacyBackupPrefs(Context context, Map<String, ?> data, boolean clearFirst) {
        if (data == null) return;
        SharedPreferences legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences currentPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (clearFirst) currentPrefs.edit().clear().apply();
        SharedPreferences.Editor legacyEditor = legacyPrefs.edit();
        if (clearFirst) legacyEditor.clear();
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean) legacyEditor.putBoolean(entry.getKey(), (Boolean) value);
            else if (value instanceof Float) legacyEditor.putFloat(entry.getKey(), (Float) value);
            else if (value instanceof Integer) legacyEditor.putInt(entry.getKey(), (Integer) value);
            else if (value instanceof Long) legacyEditor.putLong(entry.getKey(), (Long) value);
            else if (value instanceof Number) {
                double number = ((Number) value).doubleValue();
                if (number == Math.rint(number) && number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) {
                    legacyEditor.putInt(entry.getKey(), (int) number);
                } else if (number == Math.rint(number) && number >= Long.MIN_VALUE && number <= Long.MAX_VALUE) {
                    legacyEditor.putLong(entry.getKey(), (long) number);
                } else {
                    legacyEditor.putFloat(entry.getKey(), (float) number);
                }
            }
            else if (value instanceof String) legacyEditor.putString(entry.getKey(), (String) value);
        }
        legacyEditor.apply();
        if (data.containsKey(LEGACY_COINS_KEY)) {
            Object value = data.get(LEGACY_COINS_KEY);
            if (value instanceof Number) {
                int balance = Math.max(0, ((Number) value).intValue());
                currentPrefs.edit().putInt(COINS_KEY, balance).apply();
            }
        }
    }

    private void setBalance(int balance) {
        prefs.edit().putInt(COINS_KEY, balance).apply();
    }
}
