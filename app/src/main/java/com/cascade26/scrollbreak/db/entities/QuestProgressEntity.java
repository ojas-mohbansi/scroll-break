package com.cascade26.scrollbreak.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "quest_progress", primaryKeys = {"questId", "dayKey"})
public class QuestProgressEntity {
    @NonNull public String questId = "";
    @NonNull public String dayKey = "";
    public int progress;
    public boolean claimed;
}
