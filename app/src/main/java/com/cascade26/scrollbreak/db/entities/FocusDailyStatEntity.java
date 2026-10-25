package com.cascade26.scrollbreak.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "focus_daily_stats")
public class FocusDailyStatEntity {
    @PrimaryKey
    @NonNull
    public String date; // yyyy-MM-dd
    public long seconds;

    public FocusDailyStatEntity() {
        date = "";
    }
}


