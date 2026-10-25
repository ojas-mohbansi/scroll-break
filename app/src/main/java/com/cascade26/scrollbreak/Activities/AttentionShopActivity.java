package com.cascade26.scrollbreak.Activities;

import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.cascade26.scrollbreak.R;
import com.cascade26.scrollbreak.Activities.HomeActivity;
import com.cascade26.scrollbreak.Services.FocusService;
import com.cascade26.scrollbreak.Utils.ShopProgressManager;
import com.cascade26.scrollbreak.Utils.StreakManager;
import com.cascade26.scrollbreak.Utils.Utils;
import com.cascade26.scrollbreak.db.entities.AchievementEntity;
import com.cascade26.scrollbreak.db.entities.CurrencyTransactionEntity;
import com.cascade26.scrollbreak.db.entities.QuestProgressEntity;
import com.cascade26.scrollbreak.db.entities.ShopItemEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Fully local store and progression screen. */
public class AttentionShopActivity extends AppCompatActivity {
    private static final int PAGE_CATALOG = 0, PAGE_PROGRESS = 1, PAGE_HISTORY = 2;
    private ShopProgressManager manager;
    private LinearLayout content;
    private TextView balance;
    private int page = PAGE_CATALOG;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra(HomeActivity.EXTRA_START_FRAGMENT, HomeActivity.START_FRAGMENT_SHOP);
        startActivity(intent);
        finish();
    }

    @Override protected void onResume() { super.onResume(); if (manager != null) render(); }

    private void refreshStreakMilestones() {
        try { manager.refreshStreak(new StreakManager(this).getGlobalStreak()); } catch (Exception ignored) { }
    }

    private void render() {
        balance.setText(String.valueOf(manager.getBalance()));
        content.removeAllViews();
        if (page == PAGE_CATALOG) renderCatalog();
        else if (page == PAGE_PROGRESS) renderProgress();
        else renderHistory();
    }

    private void renderCatalog() {
        String section = "";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean lockedIn = prefs.getBoolean(FocusService.PREF_IS_LOCKED_IN, false);
        boolean graceUsed = ShopProgressManager.todayKey().equals(prefs.getString(ShopProgressManager.PREF_GRACE_DAY, ""));
        for (ShopProgressManager.CatalogItem item : ShopProgressManager.catalog()) {
            if (!section.equals(item.category)) {
                section = item.category;
                addSection(section.toUpperCase());
            }
            ShopItemEntity owned = manager.getOwnedItem(item.id);
            boolean alreadyOwned = owned != null;
            String actionLabel;
            boolean actionEnabled = true;
            if (item.consumable) {
                if (lockedIn) { actionLabel = "LOCK IN ACTIVE"; actionEnabled = false; }
                else if (graceUsed) { actionLabel = "USED TODAY"; actionEnabled = false; }
                else actionLabel = "USE · " + item.price;
            } else if (alreadyOwned) {
                actionLabel = owned.equipped ? "IN USE" : "APPLY";
                actionEnabled = !owned.equipped;
            } else {
                actionLabel = "GET · " + item.price;
                actionEnabled = manager.getBalance() >= item.price;
            }
            addCard(item.name, item.description, actionLabel, actionEnabled, () -> {
                if (alreadyOwned) {
                    if (manager.equip(item.id)) toast("Applied " + item.name);
                    else toast("Could not apply this item");
                    render(); return;
                }
                String confirmation = item.consumable
                        ? "Spend " + item.price + " Attention Currency to pause ordinary blocking for five minutes? Lock In stays protected."
                        : "Spend " + item.price + " Attention Currency on " + item.name + "?";
                new MaterialAlertDialogBuilder(this).setTitle(item.name).setMessage(confirmation)
                        .setNegativeButton("Keep saving", null)
                        .setPositiveButton(item.consumable ? "Use now" : "Purchase", (d, which) -> {
                            if (manager.purchaseAndApply(item.id)) toast(item.consumable ? "Grace window active for five minutes" : item.name + " added to your collection");
                            else toast("Not available: check your balance, daily limit, or Lock In status");
                            render();
                        }).show();
            });
        }
        addSection("YOUR COLLECTION");
        List<ShopItemEntity> items = manager.getOwnedItems();
        if (items.isEmpty()) addNote("Items you purchase will be saved on this device.");
        else for (ShopItemEntity item : items) {
            ShopProgressManager.CatalogItem def = getCatalogItem(item.itemId);
            if (def != null) addNote((item.equipped ? "●  " : "○  ") + def.name + (item.equipped ? " · equipped" : " · owned"));
        }
    }

    private void renderProgress() {
        refreshStreakMilestones();
        addSection("TODAY'S QUESTS");
        Map<String, QuestProgressEntity> progress = new HashMap<>();
        for (QuestProgressEntity q : manager.getTodayQuests()) progress.put(q.questId, q);
        for (String id : new String[]{"focus_once", "tasks_two", "habit_once"}) {
            QuestProgressEntity q = progress.get(id);
            int target = ShopProgressManager.questTarget(id);
            boolean ready = q != null && q.progress >= target && !q.claimed;
            String status = q == null ? "0" : String.valueOf(q.progress);
            String action = q != null && q.claimed ? "CLAIMED" : ready ? "CLAIM · +" + ShopProgressManager.questReward(id) : status + " / " + target;
            addCard(ShopProgressManager.questTitle(id), "Complete the action in Scroll Break to make progress. Reward: " + ShopProgressManager.questReward(id) + " Attention Currency.", action, ready, () -> {
                if (manager.claimQuest(id)) toast("Quest reward added");
                else toast("Keep going to complete this quest");
                render();
            });
        }
        addSection("MILESTONES & ACHIEVEMENTS");
        String[] ids = {"first_focus", "focus_10", "tasks_10", "habits_7", "streak_3", "streak_7"};
        Map<String, AchievementEntity> achievements = new HashMap<>();
        for (AchievementEntity a : manager.getAchievements()) achievements.put(a.achievementId, a);
        for (String id : ids) {
            AchievementEntity a = achievements.get(id);
            int current = a == null ? 0 : a.progress;
            boolean ready = a != null && a.unlockedAt > 0 && !a.claimed;
            String action = a != null && a.claimed ? "CLAIMED" : ready ? "CLAIM · +" + ShopProgressManager.achievementReward(id) : current + " / " + ShopProgressManager.achievementTarget(id);
            addCard(ShopProgressManager.achievementTitle(id), "Reward: " + ShopProgressManager.achievementReward(id) + " Attention Currency.", action, ready, () -> {
                if (manager.claimAchievement(id)) toast("Milestone reward added");
                else toast("This milestone is still in progress");
                render();
            });
        }
    }

    private void renderHistory() {
        addSection("RECENT ATTENTION CURRENCY ACTIVITY");
        List<CurrencyTransactionEntity> rows = manager.getRecentTransactions();
        if (rows.isEmpty()) { addNote("Your focus rewards, quest claims, purchases, and uses will appear here."); return; }
        for (CurrencyTransactionEntity row : rows) {
            String amount = (row.amount > 0 ? "+" : "") + row.amount + " AC";
            String date = android.text.format.DateFormat.format("MMM d · h:mm a", row.createdAt).toString();
            addCard(row.reason == null ? "Activity" : row.reason, date + "  ·  " + amount, null, false, null);
        }
    }

    private void addSection(String label) {
        TextView title = new TextView(this);
        title.setText(label); title.setTextColor(Color.rgb(16, 185, 129)); title.setTextSize(12); title.setLetterSpacing(.12f);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        title.setPadding(2, dp(16), 2, dp(8)); content.addView(title);
    }

    private void addNote(String text) {
        TextView line = new TextView(this); line.setText(text); line.setTextColor(Color.LTGRAY); line.setTextSize(14);
        line.setPadding(dp(4), dp(8), dp(4), dp(8)); content.addView(line);
    }

    private void addCard(String title, String detail, String action, boolean enabled, Runnable click) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2); cardLp.bottomMargin = dp(10);
        card.setLayoutParams(cardLp); card.setCardBackgroundColor(Color.rgb(16, 24, 32)); card.setStrokeColor(Color.rgb(36, 55, 64)); card.setStrokeWidth(dp(1)); card.setRadius(dp(4));
        LinearLayout column = new LinearLayout(this); column.setOrientation(LinearLayout.VERTICAL); column.setPadding(dp(16), dp(14), dp(16), dp(12));
        TextView heading = new TextView(this); heading.setText(title); heading.setTextColor(Color.WHITE); heading.setTextSize(16); heading.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView body = new TextView(this); body.setText(detail); body.setTextColor(Color.rgb(184, 196, 204)); body.setTextSize(14); body.setPadding(0, dp(5), 0, dp(8));
        column.addView(heading); column.addView(body);
        if (action != null) {
            MaterialButton button = new MaterialButton(this);
            button.setText(action); button.setTextSize(12); button.setMinHeight(dp(48)); button.setCornerRadius(dp(4)); button.setEnabled(enabled);
            button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(enabled ? Color.rgb(30, 58, 138) : Color.rgb(45, 52, 58)));
            button.setTextColor(enabled ? Color.WHITE : Color.LTGRAY);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(2); column.addView(button, lp);
            if (click != null) button.setOnClickListener(v -> click.run());
        } else {
            TextView trailing = new TextView(this); trailing.setText(action); trailing.setTextColor(Color.rgb(16, 185, 129)); trailing.setTextSize(14); trailing.setGravity(Gravity.END); column.addView(trailing);
        }
        card.addView(column); content.addView(card);
    }

    private ShopProgressManager.CatalogItem getCatalogItem(String id) {
        for (ShopProgressManager.CatalogItem item : ShopProgressManager.catalog()) if (item.id.equals(id)) return item;
        return null;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
}
