package com.mindcompanion.service;

import com.mindcompanion.model.ChatMessage;
import com.mindcompanion.model.MoodEntry;
import com.mindcompanion.model.User;
import com.mindcompanion.model.enums.SentimentType;
import com.mindcompanion.repository.ChatMessageRepository;
import com.mindcompanion.repository.MoodEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ChatMessageRepository chatMessageRepository;
    private final MoodEntryRepository moodEntryRepository;

    /**
     * Returns count of each sentiment type for a user's chat messages.
     * e.g. { "POSITIVE": 12, "NEGATIVE": 5, "NEUTRAL": 8, "CRISIS": 1 }
     */
    public Map<String, Long> getSentimentBreakdown(User user) {
        List<ChatMessage> messages = chatMessageRepository
                .findByUserIdOrderByCreatedAtAsc(user.getId());

        return messages.stream()
                .filter(m -> m.getSentiment() != null)
                .filter(m -> "USER".equals(m.getSenderType()))
                .collect(Collectors.groupingBy(
                        m -> m.getSentiment().name(),
                        Collectors.counting()
                ));
    }

    /**
     * Returns average mood score for the last N days.
     */
    public Map<String, Object> getMoodAverage(User user, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<MoodEntry> entries = moodEntryRepository
                .findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(
                        user.getId(), since);

        if (entries.isEmpty()) {
            return Map.of(
                    "averageMood", 0.0,
                    "totalEntries", 0,
                    "period", days + " days"
            );
        }

        double average = entries.stream()
                .mapToInt(MoodEntry::getMoodScore)
                .average()
                .orElse(0.0);

        return Map.of(
                "averageMood", Math.round(average * 10.0) / 10.0,
                "totalEntries", entries.size(),
                "period", days + " days"
        );
    }

    /**
     * Returns daily mood scores for the last N days — for chart rendering.
     * e.g. [{ "date": "2026-05-28", "score": 7 }, ...]
     */
    public List<Map<String, Object>> getMoodTimeline(User user, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<MoodEntry> entries = moodEntryRepository
                .findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(
                        user.getId(), since);

        return entries.stream()
                .map(entry -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("date", entry.getCreatedAt().toLocalDate().toString());
                    point.put("score", entry.getMoodScore());
                    point.put("note", entry.getNotes() != null ? entry.getNotes() : "");
                    return point;
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns total messages sent, crisis count, and average intensity.
     */
    public Map<String, Object> getSessionStats(User user) {
        List<ChatMessage> userMessages = chatMessageRepository
                .findByUserIdOrderByCreatedAtAsc(user.getId())
                .stream()
                .filter(m -> "USER".equals(m.getSenderType()))
                .collect(Collectors.toList());

        long totalMessages = userMessages.size();

        long crisisCount = userMessages.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsCrisis()))
                .count();

        double avgIntensity = userMessages.stream()
                .filter(m -> m.getIntensityScore() != null)
                .mapToDouble(ChatMessage::getIntensityScore)
                .average()
                .orElse(0.0);

        long positiveCount = userMessages.stream()
                .filter(m -> SentimentType.POSITIVE.equals(m.getSentiment()))
                .count();

        double positiveRate = totalMessages > 0
                ? Math.round((positiveCount * 100.0 / totalMessages) * 10.0) / 10.0
                : 0.0;

        return new LinkedHashMap<>() {{
            put("totalMessages", totalMessages);
            put("crisisCount", crisisCount);
            put("averageIntensity", Math.round(avgIntensity * 100.0) / 100.0);
            put("positiveRate", positiveRate);
            put("positiveMessages", positiveCount);
        }};
    }

    /**
     * Returns a full dashboard summary combining all analytics.
     */
    public Map<String, Object> getFullDashboard(User user) {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("sentimentBreakdown", getSentimentBreakdown(user));
        dashboard.put("moodAverage7Days", getMoodAverage(user, 7));
        dashboard.put("moodAverage30Days", getMoodAverage(user, 30));
        dashboard.put("moodTimeline", getMoodTimeline(user, 30));
        dashboard.put("sessionStats", getSessionStats(user));
        return dashboard;
    }
}