
package com.cascade26.scrollbreak.Utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.MalformedURLException;
import java.util.Locale;
import java.net.URL;
import java.util.regex.Pattern;


public class AdultDomainListManager {

    private static final String TAG = "AdultDomainListManager";

    // Built-in domains that must always be present (de-duplicated on insert)
    private static final String[] PREDEFINED_DOMAINS = new String[]{
            "xvideos.com",
            "xnxx.com",
            "redtube.com",
            "xhamster.com",
            "youporn.com",
            "coomer.st",
            "spankbang.com",
            "brazzers.com",
            "onlyfans.com",
            "chaturbate.com",
            "xhamster18.desi",
            "xhaccess.com",
            "xvideos.com",
            "xvideos2.com",
            "xxvideos.video",
            "xnxx.health",
            "txnhh.com",
            "xvv1deos.com",
            "amp.xhamster.com",
    };

    // Regex patterns to robustly match dynamic/subdomain variants (case-insensitive)
    // Examples covered:
    // - hi.xhamster.com, xhamster18.desi, cdn-1.xhamster3.net
    // - m.xvideos.com, xvideos2.com, static.xxvideos.video
    // - xnxx.health, de.xnxx.com, xnxx1.net
    // - hi.xhmaster.com (common variant seen in the wild)
    private static final Pattern[] ADULT_DOMAIN_PATTERNS = new Pattern[]{
            // xhamster with optional digits and any TLD, supports subdomains
            Pattern.compile("(?i)^([a-z0-9-]+\\.)*xhamster([a-z0-9-]*\\.)*[a-z]{2,}$"),
            // xvideos or xxvideos with optional digits and any TLD, supports subdomains
            Pattern.compile("(?i)^(?:[a-z0-9-]+\\.)*x{1,9}videos\\d{0,2}\\.[a-z0-9.-]+$"),
            // xnxx with optional digits and any TLD, supports subdomains
            Pattern.compile("(?i)^(?:[a-z0-9-]+\\.)*xnxx\\d{0,2}\\.[a-z0-9.-]+$")
    };

    public interface OnLocalListReadyListener {
        void onSuccess(long mergedFileBytes, @Nullable String sha256Hex, boolean deduped);

        void onError(Exception e);
    }

    public static void ensureLocalList(@NonNull Context context, @Nullable OnLocalListReadyListener listener, boolean ignored) {
        new Thread(() -> {
            try {
                long inserted = ensurePredefinedDomains(context);
                if (listener != null) listener.onSuccess(inserted, null, false);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        }, "local-domain-list").start();
    }

    // --- Utilities ---

    // Seed the local built-in domain set.

    private static String normalizeCandidate(String candidate) {
        if (TextUtils.isEmpty(candidate)) return null;
        String normalized = candidate.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith(".")) normalized = normalized.substring(1);
        while (normalized.endsWith(".")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    private static boolean isLikelyDomain(String candidate) {
        return candidate == null || candidate.length() > 253 || !candidate.contains(".")
                || candidate.contains(" ") || candidate.contains("/");
    }

    public static long ensurePredefinedDomains(@NonNull Context context) {
        DomainDatabaseHelper helper = new DomainDatabaseHelper(context);
        SQLiteDatabase db = null;
        SQLiteStatement stmt = null;
        long inserted = 0L;
        try {
            db = helper.getWritableDatabase();
            db.beginTransaction();
            stmt = db.compileStatement("INSERT OR IGNORE INTO " + DomainDatabaseHelper.TABLE_DOMAINS + " (" + DomainDatabaseHelper.COL_DOMAIN + ") VALUES (?)");
            for (String domain : PREDEFINED_DOMAINS) {
                if (TextUtils.isEmpty(domain)) continue;
                String candidate = normalizeCandidate(domain);
                if (candidate == null || candidate.isEmpty()) continue;
                if (isLikelyDomain(candidate)) continue;
                stmt.clearBindings();
                stmt.bindString(1, candidate);
                long rowId = stmt.executeInsert();
                if (rowId != -1) inserted++;
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "ensurePredefinedDomains failed", e);
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (Exception ignore) {
            }
            if (db != null) {
                try {
                    if (db.inTransaction()) db.endTransaction();
                } catch (Exception ignore) {
                }
                try {
                    db.close();
                } catch (Exception ignore) {
                }
            }
            try {
                helper.close();
            } catch (Exception ignore) {
            }
        }
        return inserted;
    }


    public static synchronized void clearCache() {
    }

    public static boolean isAdultHost(@NonNull Context context, @Nullable String host) {
        if (host == null) return false;
        String normalized = normalizeCandidate(host);
        if (normalized == null || normalized.isEmpty()) return false;
        // Quick regex-based pattern check for dynamic variants (e.g., xhamster18.desi, xvideos2.com)
        try {
            if (matchesAdultPattern(normalized)) return true;
        } catch (Exception e) {
            Log.w(TAG, "pattern check failed", e);
        }

        DomainDatabaseHelper helper = new DomainDatabaseHelper(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        // If not domain-like (no '.') treat input as a possible title/keyword and search in DB
        if (!normalized.contains(".")) {
            try {
                String keyword = normalized.replaceAll("[^a-z0-9]+", " ").trim();
                if (!keyword.isEmpty()) {
                    String[] tokens = keyword.split("\\s+");
                    for (String token : tokens) {
                        if (token.length() < 3)
                            continue; // skip very short tokens to reduce false positives
                        try (Cursor c = db.rawQuery(
                                "SELECT 1 FROM " + DomainDatabaseHelper.TABLE_DOMAINS +
                                        " WHERE " + DomainDatabaseHelper.COL_DOMAIN + " LIKE ? COLLATE NOCASE LIMIT 1",
                                new String[]{"%" + token + "%"})) {
                            if (c.moveToFirst()) return true;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "DB keyword query failed", e);
                return false;
            }
        }
        String[] parts = normalized.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = parts.length - 1; i >= 0; i--) {
            if (sb.length() == 0) sb.append(parts[i]);
            else {
                sb.insert(0, '.');
                sb.insert(0, parts[i]);
            }
            String suffix = sb.toString();
            try (Cursor c = db.rawQuery("SELECT 1 FROM " + DomainDatabaseHelper.TABLE_DOMAINS + " WHERE " + DomainDatabaseHelper.COL_DOMAIN + "=? LIMIT 1", new String[]{suffix})) {
                if (c.moveToFirst()) return true;
            } catch (Exception e) {
                Log.e(TAG, "DB query failed", e);
                return false;
            }
        }
        return false;
    }

    private static boolean matchesAdultPattern(@NonNull String host) {
        for (Pattern pattern : ADULT_DOMAIN_PATTERNS) {
            if (pattern.matcher(host).matches()) return true;
        }
        return false;
    }

    @Nullable
    public static String extractHostFromUrlText(@Nullable String urlText) {
        if (TextUtils.isEmpty(urlText)) return null;
        String s = urlText.trim();
        try {
            if (!s.startsWith("http://") && !s.startsWith("https://")) {
                s = "https://" + s;
            }
            URL u = new URL(s);
            String host = u.getHost();
            if (host == null) return null;
            // normalize
            host = host.toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) host = host.substring(4);
            while (host.startsWith(".")) host = host.substring(1);
            while (host.endsWith(".")) host = host.substring(0, host.length() - 1);
            return host;
        } catch (MalformedURLException e) {
            // not a URL; maybe raw host
            return normalizeCandidate(s);
        }
    }
}
