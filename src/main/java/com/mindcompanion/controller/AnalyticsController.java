package com.mindcompanion.controller;

import com.mindcompanion.model.User;
import com.mindcompanion.repository.UserRepository;
import com.mindcompanion.service.AnalyticsService;
import com.mindcompanion.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final GamificationService gamificationService;
    private final UserRepository userRepository;

    /**
     * GET /api/analytics/dashboard
     * Full analytics dashboard — sentiment, mood, session stats.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(
            Principal principal) {
        User user = getUser(principal);
        return ResponseEntity.ok(analyticsService.getFullDashboard(user));
    }

    /**
     * GET /api/analytics/sentiment
     * Sentiment breakdown — count of each sentiment type.
     */
    @GetMapping("/sentiment")
    public ResponseEntity<Map<String, Long>> getSentiment(
            Principal principal) {
        User user = getUser(principal);
        return ResponseEntity.ok(
                analyticsService.getSentimentBreakdown(user));
    }

    /**
     * GET /api/analytics/mood?days=7
     * Average mood score over the last N days (default 7).
     */
    @GetMapping("/mood")
    public ResponseEntity<Map<String, Object>> getMoodAverage(
            Principal principal,
            @RequestParam(defaultValue = "7") int days) {
        User user = getUser(principal);
        return ResponseEntity.ok(
                analyticsService.getMoodAverage(user, days));
    }

    /**
     * GET /api/analytics/mood/timeline?days=30
     * Daily mood scores for chart rendering.
     */
    @GetMapping("/mood/timeline")
    public ResponseEntity<List<Map<String, Object>>> getMoodTimeline(
            Principal principal,
            @RequestParam(defaultValue = "30") int days) {
        User user = getUser(principal);
        return ResponseEntity.ok(
                analyticsService.getMoodTimeline(user, days));
    }

    /**
     * GET /api/analytics/stats
     * Session stats — total messages, crisis count, positive rate.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            Principal principal) {
        User user = getUser(principal);
        return ResponseEntity.ok(
                analyticsService.getSessionStats(user));
    }

    /**
     * GET /api/analytics/gamification
     * Full gamification profile — XP, level, streak, badges.
     */
    @GetMapping("/gamification")
    public ResponseEntity<Map<String, Object>> getGamification(
            Principal principal) {
        User user = getUser(principal);
        return ResponseEntity.ok(
                gamificationService.getGamificationProfile(user));
    }

    /**
     * GET /api/analytics/badges
     * List of badges earned by the current user.
     */
    @GetMapping("/badges")
    public ResponseEntity<List<Map<String, Object>>> getBadges(
            Principal principal) {
        User user = getUser(principal);
        return ResponseEntity.ok(
                gamificationService.getUserBadges(user));
    }

    /**
     * GET /api/analytics/streak
     * Current login/activity streak in days.
     */
    @GetMapping("/streak")
    public ResponseEntity<Map<String, Integer>> getStreak(
            Principal principal) {
        User user = getUser(principal);
        int streak = gamificationService.calculateStreak(user);
        return ResponseEntity.ok(Map.of("streak", streak));
    }

    // ─── Helper ──────────────────────────────────────
    private User getUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + principal.getName()));
    }
}