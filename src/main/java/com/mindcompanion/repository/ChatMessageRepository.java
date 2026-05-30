package com.mindcompanion.repository;

import com.mindcompanion.model.ChatMessage;
import com.mindcompanion.model.enums.SentimentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    // Get all messages for a user ordered by time
    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(Long userId);

    // Get messages for a specific session
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    // Get last N messages for a user (for AI context window)
    List<ChatMessage> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    // Count crisis messages for a user
    Long countByUserIdAndIsCrisisTrue(Long userId);

    // Get all crisis messages for a user
    List<ChatMessage> findByUserIdAndIsCrisisTrue(Long userId);

    // Get messages by sentiment for a user
    List<ChatMessage> findByUserIdAndSentiment(
            Long userId, SentimentType sentiment);


    // Get messages within a date range
    @Query("SELECT m FROM ChatMessage m WHERE m.user.id = :userId " +
            "AND m.createdAt BETWEEN :start AND :end " +
            "ORDER BY m.createdAt ASC")
    List<ChatMessage> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Delete all messages for a user (GDPR right to erasure)
    void deleteAllByUserId(Long userId);

    // Delete messages older than a date (auto-purge policy)
    @Query("DELETE FROM ChatMessage m WHERE m.user.id = :userId " +
            "AND m.createdAt < :cutoffDate")
    void deleteOldMessages(
            @Param("userId") Long userId,
            @Param("cutoffDate") LocalDateTime cutoffDate);
}