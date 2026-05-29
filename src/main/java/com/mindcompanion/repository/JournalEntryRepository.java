package com.mindcompanion.repository;

import com.mindcompanion.model.JournalEntry;
import com.mindcompanion.model.enums.SentimentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository
        extends JpaRepository<JournalEntry, Long> {

    // Get all journal entries for a user ordered by date
    List<JournalEntry> findByUserIdOrderByEntryDateDesc(Long userId);

    // Get journal entry for a specific date
    Optional<JournalEntry> findByUserIdAndEntryDate(
            Long userId, LocalDate entryDate);

    // Check if user already journaled today
    boolean existsByUserIdAndEntryDate(
            Long userId, LocalDate entryDate);

    // Get last 7 entries
    List<JournalEntry> findTop7ByUserIdOrderByEntryDateDesc(Long userId);

    // Get entries by sentiment
    List<JournalEntry> findByUserIdAndSentiment(
            Long userId, SentimentType sentiment);

    // Count total entries for a user (for streak tracking)
    Long countByUserId(Long userId);

    // Get entries between two dates
    @Query("SELECT j FROM JournalEntry j WHERE j.user.id = :userId " +
            "AND j.entryDate BETWEEN :start AND :end " +
            "ORDER BY j.entryDate ASC")
    List<JournalEntry> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // Delete all entries for a user (GDPR)
    void deleteAllByUserId(Long userId);
}