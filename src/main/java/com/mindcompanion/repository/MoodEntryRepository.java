package com.mindcompanion.repository;

import com.mindcompanion.model.MoodEntry;
import com.mindcompanion.model.enums.MoodLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodEntryRepository
        extends JpaRepository<MoodEntry, Long> {

    // Get all mood entries for a user ordered by date
    List<MoodEntry> findByUserIdOrderByEntryDateDesc(Long userId);

    // Get mood entry for a specific date
    Optional<MoodEntry> findByUserIdAndEntryDate(
            Long userId, LocalDate entryDate);

    // Check if user already checked in today
    boolean existsByUserIdAndEntryDate(
            Long userId, LocalDate entryDate);

    // Get last 7 entries for weekly trend
    List<MoodEntry> findTop7ByUserIdOrderByEntryDateDesc(Long userId);

    // Get last 30 entries for monthly chart
    List<MoodEntry> findTop30ByUserIdOrderByEntryDateDesc(Long userId);

    // Get entries between two dates
    @Query("SELECT m FROM MoodEntry m WHERE m.user.id = :userId " +
            "AND m.entryDate BETWEEN :start AND :end " +
            "ORDER BY m.entryDate ASC")
    List<MoodEntry> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // Average mood score for a user
    @Query("SELECT AVG(m.moodScore) FROM MoodEntry m " +
            "WHERE m.user.id = :userId")
    Double findAverageMoodScoreByUserId(@Param("userId") Long userId);

    // Get entries by mood level
    List<MoodEntry> findByUserIdAndMoodLevel(
            Long userId, MoodLevel moodLevel);

    List<MoodEntry> findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(
            Long userId, LocalDateTime after);
    // Delete all entries for a user (GDPR)
    void deleteAllByUserId(Long userId);
}