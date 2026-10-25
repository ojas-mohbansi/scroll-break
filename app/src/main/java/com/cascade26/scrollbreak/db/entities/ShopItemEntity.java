package com.cascade26.scrollbreak.db.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shop_items")
public class ShopItemEntity {
    @PrimaryKey @NonNull public String itemId = "";
    public String category;
    public long purchasedAt;
    public boolean equipped;
}
