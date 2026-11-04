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

    @Query("SELECT * FROM session_logs WHERE evaluationSessionId = :evaluationSessionId AND evaluationGroup IS NOT NULL AND purposeCompleted IS NULL ORDER BY startedAt ASC LIMIT 1")
    SessionLogEntity getUnreportedEvaluationOutcome(String evaluationSessionId);

    @Query("UPDATE session_logs SET purposeCompleted = :completed WHERE id = :id AND purposeCompleted IS NULL")
    void recordResolvedPurposeOutcome(long id, Boolean completed);

    @Query("UPDATE session_logs SET interventionTriggered = 1, interventionAt = :at WHERE id = :id AND endedAt = 0")
    void markIntervention(long id, long at);

    @Query("UPDATE session_logs SET interventionWasShown = 1 WHERE id = :id AND endedAt = 0")
    void markInterventionShown(long id);

    @Query("UPDATE session_logs SET overrunMs = MAX(overrunMs, :overrunMs) WHERE id = :id AND endedAt = 0")
    void markOverrun(long id, long overrunMs);

    @Query("UPDATE session_logs SET exitAttempts = exitAttempts + 1 WHERE id = :id AND endedAt = 0")
    void incrementExitAttempts(long id);

    @Query("UPDATE session_logs SET longFormSurface = 1, stableContentDwellMs = MAX(stableContentDwellMs, :dwellMs) WHERE id = (SELECT id FROM session_logs WHERE packageName = :packageName AND endedAt = 0 ORDER BY startedAt DESC LIMIT 1)")
    void recordLongFormEvidence(String packageName, long dwellMs);

    @Query("UPDATE session_logs SET longFormSurface = 1 WHERE id = (SELECT id FROM session_logs WHERE packageName = :packageName AND endedAt = 0 ORDER BY startedAt DESC LIMIT 1)")
    void markLongFormSurface(String packageName);

    @Query("UPDATE session_logs SET sessionMode = :mode WHERE id = (SELECT id FROM session_logs WHERE packageName = :packageName AND endedAt = 0 ORDER BY startedAt DESC LIMIT 1)")
    void recordSessionMode(String packageName, String mode);

    @Query("UPDATE session_logs SET evaluationGroup = :groupName WHERE id = (SELECT id FROM session_logs WHERE packageName = :packageName AND endedAt = 0 ORDER BY startedAt DESC LIMIT 1)")
    void recordEvaluationGroup(String packageName, String groupName);

    @Query("UPDATE session_logs SET purposeCompleted = :completed WHERE id = :id AND endedAt = 0")
    void recordPurposeOutcome(long id, boolean completed);

    @Query("UPDATE session_logs SET userDeclaredIntent = :intent, endedAt = :endedAt, durationMs = :durationMs, interventionSuccess = :success, outcomeResolved = 1, savedTimeMs = :savedTimeMs, purposeCompleted = :purposeCompleted, interventionWasShown = 1 WHERE id = :id AND endedAt = 0")
    void finishIntervention(long id, String intent, long endedAt, long durationMs,
            boolean success, long savedTimeMs, Boolean purposeCompleted);

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

    @Query("SELECT COUNT(*) FROM session_logs WHERE overrunMs > 0")
    int getOverrunSessionCount();

    @Query("SELECT COALESCE(SUM(overrunMs), 0) FROM session_logs")
    long getTotalOverrunMs();

    @Query("SELECT COUNT(*) FROM session_logs WHERE purposeCompleted = 1")
    int getPurposeCompletedCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE purposeCompleted IS NOT NULL")
    int getPurposeOutcomeCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE sessionMode = 'long_form' AND endedAt > 0")
    int getLongFormSessionCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE sessionMode = 'long_form' AND longFormSurface = 1 AND endedAt > 0")
    int getLongFormSurfaceSessionCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE sessionMode = 'long_form' AND purposeCompleted = 1")
    int getLongFormPurposeCompletedCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE sessionMode = 'long_form' AND purposeCompleted IS NOT NULL")
    int getLongFormPurposeOutcomeCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE sessionMode = 'standard' AND purposeCompleted = 1")
    int getStandardPurposeCompletedCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE sessionMode = 'standard' AND purposeCompleted IS NOT NULL")
    int getStandardPurposeOutcomeCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE purposeCompleted = 1 AND interventionWasShown = 1")
    int getIntervenedPurposeCompletedCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE purposeCompleted IS NOT NULL AND interventionWasShown = 1")
    int getIntervenedPurposeOutcomeCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE purposeCompleted = 1 AND interventionWasShown = 0")
    int getBaselinePurposeCompletedCount();

    @Query("SELECT COUNT(*) FROM session_logs WHERE purposeCompleted IS NOT NULL AND interventionWasShown = 0")
    int getBaselinePurposeOutcomeCount();

    @Query("SELECT COUNT(DISTINCT evaluationSessionId) FROM session_logs WHERE evaluationGroup = 'intervention' AND evaluationSessionId IS NOT NULL AND purposeCompleted = 1")
    int getAssignedInterventionPurposeCompletedCount();

    @Query("SELECT COUNT(DISTINCT evaluationSessionId) FROM session_logs WHERE evaluationGroup = 'intervention' AND evaluationSessionId IS NOT NULL AND purposeCompleted IS NOT NULL")
    int getAssignedInterventionPurposeOutcomeCount();

    @Query("SELECT COUNT(DISTINCT evaluationSessionId) FROM session_logs WHERE evaluationGroup = 'intervention' AND evaluationSessionId IS NOT NULL")
    int getAssignedInterventionSessionCount();

    @Query("SELECT COUNT(DISTINCT evaluationSessionId) FROM session_logs AS completed WHERE evaluationGroup = 'intervention' AND evaluationSessionId IS NOT NULL AND endedAt > 0 AND NOT EXISTS (SELECT 1 FROM session_logs AS active WHERE active.evaluationSessionId = completed.evaluationSessionId AND active.endedAt = 0)")
    int getAssignedInterventionCompletedSessionCount();

    @Query("SELECT COUNT(DISTINCT evaluationSessionId) FROM session_logs WHERE evaluationGroup = 'control' AND evaluationSessionId IS NOT NULL AND purposeCompleted = 1")
    int getControlPurposeCompletedCount();

    @Query("SELECT COUNT(DISTINCT evaluationSessionId) FROM session_logs WHERE evaluationGroup = 'control' AND evaluationSessionId IS NOT NULL AND purposeCompleted IS NOT NULL")
    int getControlPurposeOutcomeCount();

    @Query("SELECT COUNT(DISTINCT evaluationSessionId) FROM session_logs WHERE evaluationGroup = 'control' AND evaluationSessionId IS NOT NULL")
    int getControlSessionCount();

    @Query("SELECT COUNT(DISTINCT evaluationSessionId) FROM session_logs AS completed WHERE evaluationGroup = 'control' AND evaluationSessionId IS NOT NULL AND endedAt > 0 AND NOT EXISTS (SELECT 1 FROM session_logs AS active WHERE active.evaluationSessionId = completed.evaluationSessionId AND active.endedAt = 0)")
    int getControlCompletedSessionCount();
}
