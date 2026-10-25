package com.cascade26.scrollbreak.db.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "currency_transactions")
public class CurrencyTransactionEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public int amount;
    public String reason;
    public long createdAt;
}
