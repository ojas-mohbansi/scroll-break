package com.cascade26.scrollbreak.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "achievements")
public class AchievementEntity {
    @PrimaryKey @NonNull public String achievementId = "";
    public int progress;
    public long unlockedAt;
    public boolean claimed;
}
