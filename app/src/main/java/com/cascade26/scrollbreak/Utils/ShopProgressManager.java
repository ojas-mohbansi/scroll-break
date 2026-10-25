package com.cascade26.scrollbreak.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.cascade26.scrollbreak.Services.FocusService;
import com.cascade26.scrollbreak.db.ScrollBreakRoomDatabase;
import com.cascade26.scrollbreak.db.dao.GamificationDao;
import com.cascade26.scrollbreak.db.entities.AchievementEntity;
import com.cascade26.scrollbreak.db.entities.CurrencyTransactionEntity;
import com.cascade26.scrollbreak.db.entities.QuestProgressEntity;
import com.cascade26.scrollbreak.db.entities.ShopItemEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Offline shop, reward progression, and history. All catalog content is bundled with the app. */
public final class ShopProgressManager {
    public static final String EVENT_FOCUS = "focus";
    public static final String EVENT_TASK = "task";
    public static final String EVENT_HABIT = "habit";
    public static final String CAT_AMBIENCE = "Focus ambience";
    public static final String CAT_REMINDER = "Reminder style";
    public static final String CAT_TIMER = "Timer preset";
    public static final String CAT_UTILITY = "Detox utility";
    public static final long GRACE_DURATION_MS = 5 * 60 * 1000L;
    public static final String PREF_GRACE_UNTIL = "shop_grace_until_ms";
    public static final String PREF_GRACE_DAY = "shop_grace_day";

    public static final class CatalogItem {
        public final String id, category, name, description, value;
        public final int price;
        public final boolean consumable;
        CatalogItem(String id, String category, String name, String description, int price, String value, boolean consumable) {
            this.id = id; this.category = category; this.name = name; this.description = description;
            this.price = price; this.value = value; this.consumable = consumable;
        }
    }

    private static final List<CatalogItem> CATALOG = new ArrayList<>();
    private static final Object ECONOMY_LOCK = new Object();
    static {
        CATALOG.add(new CatalogItem("ambience_ocean", CAT_AMBIENCE, "Deep Current", "A quiet blue starfield for focus sessions.", 35, "#38BDF8", false));
        CATALOG.add(new CatalogItem("ambience_moss", CAT_AMBIENCE, "Moss Signal", "A calm green focus ambience.", 40, "#A3E635", false));
        CATALOG.add(new CatalogItem("ambience_ember", CAT_AMBIENCE, "Ember Glow", "A warm amber focus ambience.", 45, "#FDBA74", false));
        CATALOG.add(new CatalogItem("reminder_gentle", CAT_REMINDER, "Gentle Prompt", "A softer, breathing-space reminder.", 20, "gentle", false));
        CATALOG.add(new CatalogItem("reminder_direct", CAT_REMINDER, "Clear Prompt", "A short, direct reminder.", 20, "direct", false));
        CATALOG.add(new CatalogItem("timer_15", CAT_TIMER, "15 minute preset", "Set the focus timer default to 15 minutes.", 15, "15", false));
        CATALOG.add(new CatalogItem("timer_45", CAT_TIMER, "45 minute preset", "Set the focus timer default to 45 minutes.", 30, "45", false));
        CATALOG.add(new CatalogItem("timer_60", CAT_TIMER, "60 minute preset", "Set the focus timer default to 60 minutes.", 45, "60", false));
        CATALOG.add(new CatalogItem("grace_5", CAT_UTILITY, "Five minute grace", "Pause ordinary app blocking for five minutes. Never available during Lock In.", 30, "5", true));
    }

    private final Context context;
    private final GamificationDao dao;
    private final AttentionCurrency currency;
    private final SharedPreferences prefs;

    public ShopProgressManager(Context context) {
        this.context = context.getApplicationContext();
        this.dao = ScrollBreakRoomDatabase.getInstance(this.context).gamificationDao();
        this.currency = new AttentionCurrency(this.context);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    public static List<CatalogItem> catalog() { return new ArrayList<>(CATALOG); }

    public synchronized int getBalance() { return currency.getBalance(); }
    public synchronized List<ShopItemEntity> getOwnedItems() { return dao.getItems(); }
    public synchronized ShopItemEntity getOwnedItem(String id) { return dao.getItem(id); }
    public synchronized List<CurrencyTransactionEntity> getRecentTransactions() { return dao.getRecentTransactions(); }
    public synchronized List<QuestProgressEntity> getTodayQuests() {
        ensureQuests();
        return dao.getQuestsForDay(todayKey());
    }
    public synchronized List<AchievementEntity> getAchievements() { return dao.getAchievements(); }

    public synchronized boolean purchaseAndApply(String itemId) {
        CatalogItem item = findItem(itemId);
        if (item == null) return false;
        if (item.consumable) return activateGrace(item);
        synchronized (ECONOMY_LOCK) {
            if (dao.getItem(itemId) != null || currency.getBalance() < item.price) return false;
            currency.deductBalance(item.price);
            record(-item.price, "Shop · " + item.name);
            dao.unequipCategory(item.category);
            ShopItemEntity owned = new ShopItemEntity();
            owned.itemId = item.id;
            owned.category = item.category;
            owned.purchasedAt = System.currentTimeMillis();
            owned.equipped = true;
            dao.saveItem(owned);
        }
        apply(item);
        return true;
    }

    public synchronized boolean equip(String itemId) {
        CatalogItem item = findItem(itemId);
        ShopItemEntity owned = dao.getItem(itemId);
        if (item == null || owned == null || item.consumable) return false;
        dao.unequipCategory(item.category);
        owned.equipped = true;
        dao.saveItem(owned);
        apply(item);
        return true;
    }

    private boolean activateGrace(CatalogItem item) {
        boolean lockedIn = FocusService.isPublicFocusRun
                && prefs.getBoolean(FocusService.PREF_IS_LOCKED_IN, false);
        lockedIn |= prefs.getBoolean(FocusService.PREF_IS_LOCKED_IN, false);
        synchronized (ECONOMY_LOCK) {
            if (lockedIn || currency.getBalance() < item.price || todayKey().equals(prefs.getString(PREF_GRACE_DAY, ""))) return false;
            currency.deductBalance(item.price);
            long now = System.currentTimeMillis();
            prefs.edit().putString(PREF_GRACE_DAY, todayKey()).putLong(PREF_GRACE_UNTIL, now + GRACE_DURATION_MS).apply();
            record(-item.price, "Shop · Five minute grace");
            return true;
        }
    }

    public static boolean isGraceActive(Context context) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        if (p.getBoolean(FocusService.PREF_IS_LOCKED_IN, false)) return false;
        return p.getLong(PREF_GRACE_UNTIL, 0L) > System.currentTimeMillis();
    }

    public synchronized boolean claimQuest(String questId) {
        synchronized (ECONOMY_LOCK) {
            ensureQuests();
            QuestProgressEntity q = dao.getQuest(questId, todayKey());
            int target = questTarget(questId);
            if (q == null || q.claimed || q.progress < target) return false;
            q.claimed = true;
            dao.saveQuest(q);
            grant(questReward(questId), "Daily quest · " + questTitle(questId), null);
            return true;
        }
    }

    public synchronized boolean claimAchievement(String achievementId) {
        synchronized (ECONOMY_LOCK) {
            AchievementEntity a = dao.getAchievement(achievementId);
            if (a == null || a.unlockedAt == 0 || a.claimed) return false;
            a.claimed = true;
            dao.saveAchievement(a);
            grant(achievementReward(achievementId), "Achievement · " + achievementTitle(achievementId), null);
            return true;
        }
    }

    public synchronized void recordActivity(Context ignored, int reward, String reason, String event) {
        synchronized (ECONOMY_LOCK) { grant(reward, reason, event); }
    }

    public synchronized void recordPenalty(int amount, String reason) {
        int value = Math.max(0, amount);
        synchronized (ECONOMY_LOCK) {
            currency.deductBalance(value);
            if (value > 0) record(-value, reason);
        }
    }

    private void grant(int amount, String reason, String event) {
        int value = Math.max(0, amount);
        if (value > 0) {
            synchronized (ECONOMY_LOCK) {
                currency.addBalance(value);
                record(value, reason);
            }
        }
        if (event == null) return;
        ensureQuests();
        for (String questId : eventQuestIds(event)) {
            QuestProgressEntity q = dao.getQuest(questId, todayKey());
            if (q != null && !q.claimed) {
                q.progress = Math.min(questTarget(questId), q.progress + 1);
                dao.saveQuest(q);
            }
        }
        incrementAchievement("first_focus", event.equals(EVENT_FOCUS));
        incrementAchievement("focus_10", event.equals(EVENT_FOCUS));
        incrementAchievement("tasks_10", event.equals(EVENT_TASK));
        incrementAchievement("habits_7", event.equals(EVENT_HABIT));
    }

    public synchronized void refreshStreak(int streak) {
        unlockThreshold("streak_3", Math.max(0, streak), 3);
        unlockThreshold("streak_7", Math.max(0, streak), 7);
    }

    public synchronized void refreshSavedStreak() {
        refreshStreak(new StreakPrefs(context).getStreak());
    }

    private void incrementAchievement(String id, boolean increment) {
        if (!increment) return;
        AchievementEntity a = dao.getAchievement(id);
        if (a == null) { a = new AchievementEntity(); a.achievementId = id; }
        a.progress = Math.min(achievementTarget(id), a.progress + 1);
        if (a.progress >= achievementTarget(id) && a.unlockedAt == 0) a.unlockedAt = System.currentTimeMillis();
        dao.saveAchievement(a);
    }

    private void unlockThreshold(String id, int current, int target) {
        AchievementEntity a = dao.getAchievement(id);
        if (a == null) { a = new AchievementEntity(); a.achievementId = id; }
        a.progress = Math.max(a.progress, Math.min(current, target));
        if (current >= target && a.unlockedAt == 0) a.unlockedAt = System.currentTimeMillis();
        dao.saveAchievement(a);
    }

    private void ensureQuests() {
        String day = todayKey();
        for (String id : new String[]{"focus_once", "tasks_two", "habit_once"}) {
            if (dao.getQuest(id, day) == null) {
                QuestProgressEntity q = new QuestProgressEntity(); q.questId = id; q.dayKey = day; q.progress = 0; q.claimed = false;
                dao.saveQuest(q);
            }
        }
    }

    private void record(int amount, String reason) {
        CurrencyTransactionEntity t = new CurrencyTransactionEntity();
        t.amount = amount; t.reason = reason; t.createdAt = System.currentTimeMillis();
        dao.addTransaction(t);
    }

    private void apply(CatalogItem item) {
        switch (item.category) {
            case CAT_AMBIENCE:
                prefs.edit().putString("shop_focus_palette", item.value).apply(); break;
            case CAT_REMINDER:
                prefs.edit().putString("shop_reminder_style", item.value).apply(); break;
            case CAT_TIMER:
                prefs.edit().putInt("pref_focus_duration", Integer.parseInt(item.value)).apply(); break;
        }
    }

    private static CatalogItem findItem(String id) {
        for (CatalogItem item : CATALOG) if (item.id.equals(id)) return item;
        return null;
    }

    private static String[] eventQuestIds(String event) {
        switch (event) {
            case EVENT_FOCUS: return new String[]{"focus_once"};
            case EVENT_TASK: return new String[]{"tasks_two"};
            case EVENT_HABIT: return new String[]{"habit_once"};
            default: return new String[0];
        }
    }

    public static String questTitle(String id) {
        switch (id) { case "focus_once": return "Complete one focus session"; case "tasks_two": return "Finish two tasks"; default: return "Check in on one habit"; }
    }
    public static int questTarget(String id) { return "tasks_two".equals(id) ? 2 : 1; }
    public static int questReward(String id) { return "habit_once".equals(id) ? 6 : 5; }
    public static String achievementTitle(String id) {
        switch (id) { case "first_focus": return "First pause · Focus session"; case "focus_10": return "Steady mind · 10 focus sessions"; case "tasks_10": return "Small steps · 10 tasks"; case "habits_7": return "Daily rhythm · 7 habit check-ins"; case "streak_3": return "Three day rhythm"; default: return "A week of intention"; }
    }
    public static int achievementTarget(String id) {
        switch (id) { case "first_focus": return 1; case "focus_10": return 10; case "tasks_10": return 10; case "habits_7": return 7; case "streak_3": return 3; default: return 7; }
    }
    public static int achievementReward(String id) { return "first_focus".equals(id) ? 10 : ("streak_3".equals(id) ? 15 : 25); }
    public static String todayKey() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }
}
