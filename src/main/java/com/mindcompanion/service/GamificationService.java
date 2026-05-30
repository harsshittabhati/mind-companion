package com.mindcompanion.service;

import com.mindcompanion.model.Badge;
import com.mindcompanion.model.MoodEntry;
import com.mindcompanion.model.User;
import com.mindcompanion.model.UserBadge;
import com.mindcompanion.repository.BadgeRepository;
import com.mindcompanion.repository.ChatMessageRepository;
import com.mindcompanion.repository.MoodEntryRepository;
import com.mindcompanion.repository.UserBadgeRepository;
import com.mindcompanion.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserRepository userRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MoodEntryRepository moodEntryRepository;

    // XP values for different actions
    private static final int XP_PER_MESSAGE = 5;
    private static final int XP_PER_MOOD_CHECKIN = 10;
    private static final int XP_PER_JOURNAL_ENTRY = 15;
    private static final int XP_PER_STREAK_DAY = 20;

    /**
     * Awards XP to a user for a specific action.
     * Saves updated XP and checks for new badges.
     */
    @Transactional
    public void awardXp(User user, String action) {
        int xpToAdd = switch (action) {
            case "MESSAGE" -> XP_PER_MESSAGE;
            case "MOOD_CHECKIN" -> XP_PER_MOOD_CHECKIN;
            case "JOURNAL_ENTRY" -> XP_PER_JOURNAL_ENTRY;
            case "STREAK_DAY" -> XP_PER_STREAK_DAY;
            default -> 0;
        };

        if (xpToAdd == 0) return;

        user.setXpPoints(user.getXpPoints() + xpToAdd);
        userRepository.save(user);

        log.debug("Awarded {} XP to user '{}' for action '{}'. Total: {}",
                xpToAdd, user.getUsername(), action, user.getXpPoints());

        // Check if any new badges should be awarded
        checkAndAwardBadges(user);
    }

    /**
     * Returns the current XP level and progress for a user.
     * Level formula: level = XP / 100 (every 100 XP = 1 level)
     */
    public Map<String, Object> getXpStatus(User user) {
        int xp = user.getXpPoints() != null ? user.getXpPoints() : 0;
        int level = xp / 100;
        int xpInCurrentLevel = xp % 100;
        int xpToNextLevel = 100 - xpInCurrentLevel;

        return new LinkedHashMap<>() {{
            put("totalXp", xp);
            put("level", level);
            put("xpInCurrentLevel", xpInCurrentLevel);
            put("xpToNextLevel", xpToNextLevel);
            put("levelTitle", getLevelTitle(level));
        }};
    }

    /**
     * Calculates the user's current streak (consecutive days with activity).
     */
    public int calculateStreak(User user) {
        List<MoodEntry> entries = moodEntryRepository
                .findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(
                        user.getId(),
                        LocalDateTime.now().minusDays(365));

        if (entries.isEmpty()) return 0;

        // Get distinct dates with activity
        Set<LocalDate> activeDates = entries.stream()
                .map(e -> e.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());

        // Count consecutive days ending today
        int streak = 0;
        LocalDate date = LocalDate.now();

        while (activeDates.contains(date)) {
            streak++;
            date = date.minusDays(1);
        }

        return streak;
    }

    /**
     * Returns all badges earned by the user.
     */
    public List<Map<String, Object>> getUserBadges(User user) {
        List<UserBadge> userBadges = userBadgeRepository
                .findByUserOrderByEarnedAtDesc(user);

        return userBadges.stream()
                .map(ub -> {
                    Map<String, Object> badgeMap = new LinkedHashMap<>();
                    badgeMap.put("id", ub.getBadge().getId());
                    badgeMap.put("name", ub.getBadge().getName());
                    badgeMap.put("description", ub.getBadge().getDescription());
                    badgeMap.put("icon", ub.getBadge().getIcon());
                    badgeMap.put("earnedAt", ub.getEarnedAt().toString());
                    return badgeMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns the full gamification profile for a user.
     */
    public Map<String, Object> getGamificationProfile(User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("xpStatus", getXpStatus(user));
        profile.put("streak", calculateStreak(user));
        profile.put("badges", getUserBadges(user));
        profile.put("badgeCount", getUserBadges(user).size());
        return profile;
    }

    // ─── Private helpers ─────────────────────────────

    /**
     * Checks XP milestones and awards badges automatically.
     */
    @Transactional
    private void checkAndAwardBadges(User user) {
        int xp = user.getXpPoints() != null ? user.getXpPoints() : 0;
        List<String> alreadyEarned = userBadgeRepository
                .findByUserOrderByEarnedAtDesc(user)
                .stream()
                .map(ub -> ub.getBadge().getName())
                .collect(Collectors.toList());

        // XP milestone badges
        awardBadgeIfEligible(user, "First Steps", xp >= 10, alreadyEarned);
        awardBadgeIfEligible(user, "Getting Started", xp >= 50, alreadyEarned);
        awardBadgeIfEligible(user, "Committed", xp >= 100, alreadyEarned);
        awardBadgeIfEligible(user, "Dedicated", xp >= 250, alreadyEarned);
        awardBadgeIfEligible(user, "Champion", xp >= 500, alreadyEarned);

        // Streak badges
        int streak = calculateStreak(user);
        awardBadgeIfEligible(user, "3-Day Streak", streak >= 3, alreadyEarned);
        awardBadgeIfEligible(user, "Week Warrior", streak >= 7, alreadyEarned);
        awardBadgeIfEligible(user, "Monthly Master", streak >= 30, alreadyEarned);
    }

    private void awardBadgeIfEligible(User user, String badgeName,
                                      boolean condition,
                                      List<String> alreadyEarned) {
        if (!condition || alreadyEarned.contains(badgeName)) return;

        badgeRepository.findByName(badgeName).ifPresent(badge -> {
            UserBadge userBadge = UserBadge.builder()
                    .user(user)
                    .badge(badge)
                    .earnedAt(LocalDateTime.now())
                    .build();
            userBadgeRepository.save(userBadge);
            log.info("🏆 Badge '{}' awarded to user '{}'",
                    badgeName, user.getUsername());
        });
    }

    private String getLevelTitle(int level) {
        return switch (level) {
            case 0 -> "Newcomer";
            case 1 -> "Explorer";
            case 2 -> "Seeker";
            case 3 -> "Journeyer";
            case 4 -> "Pathfinder";
            case 5 -> "Mindful";
            case 6 -> "Resilient";
            case 7 -> "Warrior";
            case 8 -> "Guardian";
            case 9 -> "Sage";
            default -> level >= 10 ? "Enlightened" : "Newcomer";
        };
    }
}