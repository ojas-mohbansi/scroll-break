package com.cascade26.scrollbreak.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "blocklist", indices = {@Index(value = {"domain"}, unique = true)})
public class BlocklistEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String domain = "";

    public String category; // "ad", "adult", "scam", "malware", "social"
}
