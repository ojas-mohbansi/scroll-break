package com.cascade26.scrollbreak.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.cascade26.scrollbreak.db.entities.AchievementEntity;
import com.cascade26.scrollbreak.db.entities.CurrencyTransactionEntity;
import com.cascade26.scrollbreak.db.entities.QuestProgressEntity;
import com.cascade26.scrollbreak.db.entities.ShopItemEntity;

import java.util.List;

@Dao
public interface GamificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) void saveItem(ShopItemEntity item);
    @Query("SELECT * FROM shop_items ORDER BY purchasedAt DESC") List<ShopItemEntity> getItems();
    @Query("SELECT * FROM shop_items WHERE itemId = :itemId LIMIT 1") ShopItemEntity getItem(String itemId);
    @Query("UPDATE shop_items SET equipped = 0 WHERE category = :category") void unequipCategory(String category);
    @Query("DELETE FROM shop_items") void clearItems();

    @Insert(onConflict = OnConflictStrategy.REPLACE) void saveQuest(QuestProgressEntity quest);
    @Query("SELECT * FROM quest_progress WHERE dayKey = :dayKey") List<QuestProgressEntity> getQuestsForDay(String dayKey);
    @Query("SELECT * FROM quest_progress") List<QuestProgressEntity> getAllQuests();
    @Query("SELECT * FROM quest_progress WHERE questId = :questId AND dayKey = :dayKey LIMIT 1") QuestProgressEntity getQuest(String questId, String dayKey);
    @Query("DELETE FROM quest_progress") void clearQuests();

    @Insert(onConflict = OnConflictStrategy.REPLACE) void saveAchievement(AchievementEntity achievement);
    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC") List<AchievementEntity> getAchievements();
    @Query("SELECT * FROM achievements WHERE achievementId = :id LIMIT 1") AchievementEntity getAchievement(String id);
    @Query("DELETE FROM achievements") void clearAchievements();

    @Insert(onConflict = OnConflictStrategy.REPLACE) void addTransaction(CurrencyTransactionEntity transaction);
    @Query("SELECT * FROM currency_transactions ORDER BY createdAt DESC LIMIT 50") List<CurrencyTransactionEntity> getRecentTransactions();
    @Query("SELECT * FROM currency_transactions ORDER BY createdAt DESC") List<CurrencyTransactionEntity> getAllTransactions();
    @Query("DELETE FROM currency_transactions") void clearTransactions();
}
