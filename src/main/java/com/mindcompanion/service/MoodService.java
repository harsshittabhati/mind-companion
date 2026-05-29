package com.mindcompanion.service;

import com.mindcompanion.dto.request.MoodRequest;
import com.mindcompanion.dto.response.MoodResponse;
import com.mindcompanion.model.MoodEntry;
import com.mindcompanion.model.User;
import com.mindcompanion.model.enums.MoodLevel;
import com.mindcompanion.repository.MoodEntryRepository;
import com.mindcompanion.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoodService {

    private final MoodEntryRepository moodEntryRepository;
    private final UserRepository userRepository;

    // ─── Save today's mood check-in ─────────────────
    @Transactional
    public MoodResponse saveMoodEntry(MoodRequest request,
                                      String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + username));

        LocalDate today = LocalDate.now();

        // Check if already checked in today
        if (moodEntryRepository.existsByUserIdAndEntryDate(
                user.getId(), today)) {
            // Update existing entry instead
            MoodEntry existing = moodEntryRepository
                    .findByUserIdAndEntryDate(user.getId(), today)
                    .orElseThrow();
            existing.setMoodScore(request.getMoodScore());
            existing.setMoodLevel(
                    mapScoreToLevel(request.getMoodScore()));
            existing.setNotes(request.getNotes());
            MoodEntry saved = moodEntryRepository.save(existing);
            log.debug("Updated mood entry for user: {}", username);
            return buildResponse(saved, user.getId());
        }

        // Create new entry
        MoodEntry entry = MoodEntry.builder()
                .moodScore(request.getMoodScore())
                .moodLevel(mapScoreToLevel(request.getMoodScore()))
                .notes(request.getNotes())
                .aiInsight(generateInsight(
                        request.getMoodScore(), user.getId()))
                .entryDate(today)
                .user(user)
                .build();

        MoodEntry saved = moodEntryRepository.save(entry);
        log.debug("Saved mood entry for user: {}", username);

        return buildResponse(saved, user.getId());
    }

    // ─── Get today's mood entry ──────────────────────
    public MoodResponse getTodayMood(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + username));

        return moodEntryRepository
                .findByUserIdAndEntryDate(
                        user.getId(), LocalDate.now())
                .map(entry -> buildResponse(entry, user.getId()))
                .orElse(null);
    }

    // ─── Get mood history ────────────────────────────
    public List<MoodResponse> getMoodHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + username));

        return moodEntryRepository
                .findByUserIdOrderByEntryDateDesc(user.getId())
                .stream()
                .map(entry -> buildResponse(entry, user.getId()))
                .collect(Collectors.toList());
    }

    // ─── Get last 7 days for weekly chart ───────────
    public List<MoodResponse> getWeeklyMood(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + username));

        return moodEntryRepository
                .findTop7ByUserIdOrderByEntryDateDesc(user.getId())
                .stream()
                .map(entry -> buildResponse(entry, user.getId()))
                .collect(Collectors.toList());
    }

    // ─── Map score 1-10 to MoodLevel enum ───────────
    public MoodLevel mapScoreToLevel(int score) {
        if (score <= 2) return MoodLevel.VERY_LOW;
        if (score <= 4) return MoodLevel.LOW;
        if (score <= 6) return MoodLevel.NEUTRAL;
        if (score <= 8) return MoodLevel.GOOD;
        return MoodLevel.EXCELLENT;
    }

    // ─── Generate AI insight based on mood ──────────
    private String generateInsight(int score, Long userId) {
        // Get recent average for comparison
        Double avg = moodEntryRepository
                .findAverageMoodScoreByUserId(userId);

        if (avg == null) {
            return "Welcome! This is your first mood check-in. " +
                    "Tracking your mood daily helps identify patterns.";
        }

        if (score <= 3) {
            return "You're going through a tough time. " +
                    "Remember to be kind to yourself today. " +
                    "Try a 5-minute breathing exercise.";
        } else if (score <= 5) {
            return "You're feeling below your usual baseline. " +
                    "A short walk or journaling might help lift your mood.";
        } else if (score <= 7) {
            return "You're feeling okay today. " +
                    "Keep up your healthy routines!";
        } else {
            return "Great mood today! " +
                    "Take note of what contributed to this — " +
                    "it can help you recreate these conditions.";
        }
    }

    // ─── Build MoodResponse from MoodEntry ──────────
    private MoodResponse buildResponse(MoodEntry entry, Long userId) {
        Double avg = moodEntryRepository
                .findAverageMoodScoreByUserId(userId);

        return MoodResponse.builder()
                .id(entry.getId())
                .moodScore(entry.getMoodScore())
                .moodLevel(entry.getMoodLevel())
                .notes(entry.getNotes())
                .aiInsight(entry.getAiInsight())
                .entryDate(entry.getEntryDate())
                .createdAt(entry.getCreatedAt())
                .averageMoodScore(avg != null
                        ? Math.round(avg * 10.0) / 10.0 : 0.0)
                .build();
    }
}