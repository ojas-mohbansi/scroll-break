package com.cascade26.scrollbreak.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.cascade26.scrollbreak.db.entities.BlocklistEntity;

import java.util.List;

@Dao
public interface BlocklistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BlocklistEntity entry);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BlocklistEntity> entries);

    @Query("SELECT category FROM blocklist WHERE domain = :domain LIMIT 1")
    String getCategoryForDomain(String domain);

    @Query("SELECT EXISTS(SELECT 1 FROM blocklist WHERE domain = :domain LIMIT 1)")
    boolean isBlocked(String domain);

    @Query("DELETE FROM blocklist")
    void clearTable();

    @Query("SELECT COUNT(*) FROM blocklist")
    int getCount();
}
