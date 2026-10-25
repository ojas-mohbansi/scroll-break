package com.cascade26.scrollbreak.Fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.cascade26.scrollbreak.R;
import com.cascade26.scrollbreak.Services.FocusService;
import com.cascade26.scrollbreak.Utils.ShopProgressManager;
import com.cascade26.scrollbreak.Utils.StreakManager;
import com.cascade26.scrollbreak.db.entities.AchievementEntity;
import com.cascade26.scrollbreak.db.entities.CurrencyTransactionEntity;
import com.cascade26.scrollbreak.db.entities.QuestProgressEntity;
import com.cascade26.scrollbreak.db.entities.ShopItemEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Offline shop, quests, and history inside the shared HomeActivity navigation shell. */
public class ShopFragment extends Fragment {
    private static final int PAGE_CATALOG = 0;
    private static final int PAGE_PROGRESS = 1;
    private static final int PAGE_HISTORY = 2;

    private ShopProgressManager manager;
    private LinearLayout content;
    private TextView balance;
    private int page = PAGE_CATALOG;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shop, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        manager = new ShopProgressManager(requireContext());
        content = view.findViewById(R.id.shopContent);
        balance = view.findViewById(R.id.shopBalance);
        MaterialButtonToggleGroup tabs = view.findViewById(R.id.shopTabs);
        tabs.check(R.id.tabCatalog);
        tabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            page = checkedId == R.id.tabProgress ? PAGE_PROGRESS
                    : checkedId == R.id.tabHistory ? PAGE_HISTORY : PAGE_CATALOG;
            render();
        });
        refreshStreakMilestones();
        render();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (manager != null && content != null) render();
    }

    private void refreshStreakMilestones() {
        try {
            manager.refreshStreak(new StreakManager(requireContext()).getGlobalStreak());
        } catch (Exception ignored) {
            // Shop remains available offline even if streak data is unavailable.
        }
    }

    private void render() {
        if (balance == null || content == null || manager == null) return;
        balance.setText(String.valueOf(manager.getBalance()));
        content.removeAllViews();
        if (page == PAGE_CATALOG) renderCatalog();
        else if (page == PAGE_PROGRESS) renderProgress();
        else renderHistory();
    }

    private void renderCatalog() {
        String section = "";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean lockedIn = prefs.getBoolean(FocusService.PREF_IS_LOCKED_IN, false);
        boolean graceUsed = ShopProgressManager.todayKey().equals(
                prefs.getString(ShopProgressManager.PREF_GRACE_DAY, ""));
        for (ShopProgressManager.CatalogItem item : ShopProgressManager.catalog()) {
            if (!section.equals(item.category)) {
                section = item.category;
                addSection(section.toUpperCase(Locale.getDefault()));
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
                    render();
                    return;
                }
                String confirmation = item.consumable
                        ? "Spend " + item.price + " Attention Currency to pause ordinary blocking for five minutes? Lock In stays protected."
                        : "Spend " + item.price + " Attention Currency on " + item.name + "?";
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(item.name)
                        .setMessage(confirmation)
                        .setNegativeButton("Keep saving", null)
                        .setPositiveButton(item.consumable ? "Use now" : "Purchase", (dialog, which) -> {
                            if (manager.purchaseAndApply(item.id)) {
                                toast(item.consumable ? "Grace window active for five minutes"
                                        : item.name + " added to your collection");
                            } else {
                                toast("Not available: check your balance, daily limit, or Lock In status");
                            }
                            render();
                        }).show();
            });
        }
        addSection("YOUR COLLECTION");
        List<ShopItemEntity> items = manager.getOwnedItems();
        if (items.isEmpty()) addNote("Items you purchase will be saved on this device.");
        else for (ShopItemEntity item : items) {
            ShopProgressManager.CatalogItem definition = getCatalogItem(item.itemId);
            if (definition != null) {
                addNote((item.equipped ? "●  " : "○  ") + definition.name
                        + (item.equipped ? " · equipped" : " · owned"));
            }
        }
    }

    private void renderProgress() {
        refreshStreakMilestones();
        addSection("TODAY'S QUESTS");
        Map<String, QuestProgressEntity> progress = new HashMap<>();
        for (QuestProgressEntity quest : manager.getTodayQuests()) progress.put(quest.questId, quest);
        for (String id : new String[]{"focus_once", "tasks_two", "habit_once"}) {
            QuestProgressEntity quest = progress.get(id);
            int target = ShopProgressManager.questTarget(id);
            boolean ready = quest != null && quest.progress >= target && !quest.claimed;
            String status = quest == null ? "0" : String.valueOf(quest.progress);
            String action = quest != null && quest.claimed ? "CLAIMED"
                    : ready ? "CLAIM · +" + ShopProgressManager.questReward(id) : status + " / " + target;
            addCard(ShopProgressManager.questTitle(id),
                    "Complete the action in Scroll Break to make progress. Reward: "
                            + ShopProgressManager.questReward(id) + " Attention Currency.",
                    action, ready, () -> {
                        if (manager.claimQuest(id)) toast("Quest reward added");
                        else toast("Keep going to complete this quest");
                        render();
                    });
        }
        addSection("MILESTONES & ACHIEVEMENTS");
        String[] ids = {"first_focus", "focus_10", "tasks_10", "habits_7", "streak_3", "streak_7"};
        Map<String, AchievementEntity> achievements = new HashMap<>();
        for (AchievementEntity achievement : manager.getAchievements()) {
            achievements.put(achievement.achievementId, achievement);
        }
        for (String id : ids) {
            AchievementEntity achievement = achievements.get(id);
            int current = achievement == null ? 0 : achievement.progress;
            boolean ready = achievement != null && achievement.unlockedAt > 0 && !achievement.claimed;
            String action = achievement != null && achievement.claimed ? "CLAIMED"
                    : ready ? "CLAIM · +" + ShopProgressManager.achievementReward(id)
                    : current + " / " + ShopProgressManager.achievementTarget(id);
            addCard(ShopProgressManager.achievementTitle(id),
                    "Reward: " + ShopProgressManager.achievementReward(id) + " Attention Currency.",
                    action, ready, () -> {
                        if (manager.claimAchievement(id)) toast("Milestone reward added");
                        else toast("This milestone is still in progress");
                        render();
                    });
        }
    }

    private void renderHistory() {
        addSection("RECENT ATTENTION CURRENCY ACTIVITY");
        List<CurrencyTransactionEntity> rows = manager.getRecentTransactions();
        if (rows.isEmpty()) {
            addNote("Your focus rewards, quest claims, purchases, and uses will appear here.");
            return;
        }
        for (CurrencyTransactionEntity row : rows) {
            String amount = (row.amount > 0 ? "+" : "") + row.amount + " AC";
            String date = android.text.format.DateFormat.format("MMM d · h:mm a", row.createdAt).toString();
            addCard(row.reason == null ? "Activity" : row.reason, date + "  ·  " + amount, null, false, null);
        }
    }

    private void addSection(String label) {
        TextView title = new TextView(requireContext());
        title.setText(label);
        title.setTextColor(MaterialColors.getColor(title, R.attr.brand_accent));
        title.setTextSize(12);
        title.setLetterSpacing(.12f);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        title.setPadding(dp(2), dp(16), dp(2), dp(8));
        content.addView(title);
    }

    private void addNote(String text) {
        TextView line = new TextView(requireContext());
        line.setText(text);
        line.setTextColor(MaterialColors.getColor(line, R.attr.text_secondary));
        line.setTextSize(14);
        line.setPadding(dp(4), dp(8), dp(4), dp(8));
        content.addView(line);
    }

    private void addCard(String title, String detail, String action, boolean enabled, Runnable click) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(MaterialColors.getColor(card, R.attr.surface_card));
        card.setStrokeColor(MaterialColors.getColor(card, R.attr.glass_stroke));
        card.setStrokeWidth(dp(1));
        card.setRadius(getResources().getDimension(R.dimen.component_corner_radius));

        LinearLayout column = new LinearLayout(requireContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(16), dp(14), dp(16), dp(12));
        TextView heading = new TextView(requireContext());
        heading.setText(title);
        heading.setTextColor(MaterialColors.getColor(heading, R.attr.text_primary));
        heading.setTextSize(16);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView body = new TextView(requireContext());
        body.setText(detail);
        body.setTextColor(MaterialColors.getColor(body, R.attr.text_secondary));
        body.setTextSize(14);
        body.setPadding(0, dp(5), 0, action == null ? 0 : dp(8));
        column.addView(heading);
        column.addView(body);
        if (action != null) {
            MaterialButton button = new MaterialButton(requireContext());
            button.setText(action);
            button.setTextSize(12);
            button.setMinHeight(dp(48));
            button.setCornerRadius(Math.round(getResources().getDimension(R.dimen.control_corner_radius)));
            button.setEnabled(enabled);
            if (click != null && enabled) button.setOnClickListener(v -> click.run());
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            buttonParams.topMargin = dp(2);
            column.addView(button, buttonParams);
        }
        card.addView(column);
        content.addView(card);
    }

    private ShopProgressManager.CatalogItem getCatalogItem(String id) {
        for (ShopProgressManager.CatalogItem item : ShopProgressManager.catalog()) {
            if (item.id.equals(id)) return item;
        }
        return null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
