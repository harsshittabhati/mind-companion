package com.mindcompanion.repository;

import java.time.LocalDateTime;
import com.mindcompanion.model.ChatMessage;
import com.mindcompanion.model.enums.SentimentType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
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

    // Get recent messages for current session + non-crisis messages from last 24h
    @Query("SELECT m FROM ChatMessage m WHERE m.user.id = :userId " +
            "AND (m.sessionId = :sessionId OR " +
            "(m.isCrisis = false AND m.createdAt > :since)) " +
            "ORDER BY m.createdAt DESC")
    org.springframework.data.domain.Page<ChatMessage> findContextMessages(
            @Param("userId") Long userId,
            @Param("sessionId") String sessionId,
            @Param("since") LocalDateTime since,
            org.springframework.data.domain.Pageable pageable);

    // Check if user sent any message today (for first-message-of-day XP bonus)
    boolean existsByUserIdAndSenderTypeAndCreatedAtAfter(
            Long userId, String senderType, LocalDateTime after);

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

    @Modifying
    @Transactional
    @Query("DELETE FROM ChatMessage m WHERE m.user.id = :userId AND m.createdAt < :cutoff")
    int deleteByUserIdAndCreatedAtBefore(@Param("userId") Long userId,
                                         @Param("cutoff") LocalDateTime cutoff);
}