package com.cascade26.scrollbreak.db.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Local, content-free outcome record for one monitored-app foreground session. */
@Entity(tableName = "session_logs")
public class SessionLogEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String packageName;

    @NonNull
    public String appName;

    @Nullable
    public String userDeclaredIntent;

    public long startedAt;
    public long endedAt;
    public long durationMs;
    public long interventionAt;
    public boolean interventionTriggered;
    public boolean interventionSuccess;
    public boolean outcomeResolved;
    public long sessionGoalMs;
    public long savedTimeMs;

    public SessionLogEntity(@NonNull String packageName, @NonNull String appName,
            long startedAt, long sessionGoalMs) {
        this.packageName = packageName;
        this.appName = appName;
        this.startedAt = startedAt;
        this.sessionGoalMs = sessionGoalMs;
        this.userDeclaredIntent = null;
        this.endedAt = 0L;
        this.durationMs = 0L;
        this.interventionAt = 0L;
        this.interventionTriggered = false;
        this.interventionSuccess = false;
        this.outcomeResolved = false;
        this.savedTimeMs = 0L;
    }
}
