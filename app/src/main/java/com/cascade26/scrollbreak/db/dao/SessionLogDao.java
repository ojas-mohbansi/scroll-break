package com.cascade26.scrollbreak.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.cascade26.scrollbreak.db.entities.SessionLogEntity;

@Dao
public interface SessionLogDao {
    @Insert
    long insert(SessionLogEntity session);

    @Query("SELECT * FROM session_logs WHERE packageName = :packageName AND endedAt = 0 ORDER BY startedAt DESC LIMIT 1")
    SessionLogEntity getActive(String packageName);

    @Query("UPDATE session_logs SET interventionTriggered = 1, interventionAt = :at WHERE id = :id AND endedAt = 0")
    void markIntervention(long id, long at);

    @Query("UPDATE session_logs SET userDeclaredIntent = :intent, endedAt = :endedAt, durationMs = :durationMs, interventionSuccess = :success, outcomeResolved = 1, savedTimeMs = :savedTimeMs WHERE id = :id AND endedAt = 0")
    void finishIntervention(long id, String intent, long endedAt, long durationMs,
            boolean success, long savedTimeMs);

    @Query("UPDATE session_logs SET endedAt = :endedAt, durationMs = :durationMs, outcomeResolved = 1 WHERE id = :id AND endedAt = 0")
    void finishWithoutIntervention(long id, long endedAt, long durationMs);

    @Query("UPDATE session_logs SET endedAt = :endedAt, durationMs = MAX(0, :endedAt - startedAt), interventionSuccess = 0, outcomeResolved = 1 WHERE endedAt = 0")
    void closeAbandonedSessions(long endedAt);

    @Query("SELECT COALESCE(SUM(savedTimeMs), 0) FROM session_logs WHERE interventionTriggered = 1 AND interventionSuccess = 1")
    long getTotalSavedTimeMs();

    @Query("SELECT COUNT(*) FROM session_logs WHERE interventionTriggered = 1 AND outcomeResolved = 1")
    int getResolvedInterventionCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE interventionTriggered = 1 AND outcomeResolved = 1 AND interventionSuccess = 1")
    int getSuccessfulInterventionCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE interventionTriggered = 1 AND userDeclaredIntent IS NOT NULL AND TRIM(userDeclaredIntent) != '' AND outcomeResolved = 1")
    int getIntentDeclaredInterventionCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE interventionTriggered = 1 AND userDeclaredIntent IS NOT NULL AND TRIM(userDeclaredIntent) != '' AND outcomeResolved = 1 AND interventionSuccess = 1")
    int getIntentDeclaredSuccessfulCount();
}
