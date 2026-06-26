package com.mindcompanion.service;

import com.mindcompanion.model.Badge;
import com.mindcompanion.model.MoodEntry;
import com.mindcompanion.model.User;
import com.mindcompanion.model.UserBadge;
import com.mindcompanion.model.enums.SentimentType;
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

    // XP values
    private static final int XP_PER_MESSAGE           = 2;
    private static final int XP_POSITIVE_SENTIMENT    = 3;
    private static final int XP_FIRST_MESSAGE_OF_DAY  = 5;
    private static final int XP_PER_MOOD_CHECKIN      = 10;
    private static final int XP_SHORT_JOURNAL         = 15;
    private static final int XP_LONG_JOURNAL          = 20;
    private static final int XP_PER_STREAK_DAY        = 20;
    private static final int XP_WEEKLY_MOOD_STREAK    = 50;
    private static final int XP_CRISIS_RECOVERY       = 10;

    @Transactional
    public void awardXp(User user, String action) {
        awardXp(user, action, null, 0);
    }

    @Transactional
    public void awardXp(User user, String action, SentimentType sentiment, int wordCount) {
        int xpToAdd = 0;

        switch (action) {
            case "MESSAGE" -> {
                xpToAdd += XP_PER_MESSAGE;

                // Bonus: positive sentiment
                if (sentiment == SentimentType.POSITIVE) {
                    xpToAdd += XP_POSITIVE_SENTIMENT;
                    log.debug("Positive sentiment bonus +{} XP", XP_POSITIVE_SENTIMENT);
                }

                // Bonus: crisis recovery (positive after recent crisis)
                if (sentiment == SentimentType.POSITIVE && hadRecentCrisis(user)) {
                    xpToAdd += XP_CRISIS_RECOVERY;
                    log.debug("Crisis recovery bonus +{} XP", XP_CRISIS_RECOVERY);
                }

                // Bonus: first message of the day
                if (isFirstMessageToday(user)) {
                    xpToAdd += XP_FIRST_MESSAGE_OF_DAY;
                    log.debug("First message of day bonus +{} XP", XP_FIRST_MESSAGE_OF_DAY);
                }
            }
            case "MOOD_CHECKIN" -> {
                xpToAdd += XP_PER_MOOD_CHECKIN;

                // Bonus: weekly mood streak (7 consecutive days)
                if (hasWeeklyMoodStreak(user)) {
                    xpToAdd += XP_WEEKLY_MOOD_STREAK;
                    log.debug("Weekly mood streak bonus +{} XP", XP_WEEKLY_MOOD_STREAK);
                }
            }
            case "JOURNAL_ENTRY" -> {
                // Long entry (150+ words) gets more XP
                xpToAdd += (wordCount >= 150) ? XP_LONG_JOURNAL : XP_SHORT_JOURNAL;
            }
            case "STREAK_DAY" -> xpToAdd += XP_PER_STREAK_DAY;
        }

        if (xpToAdd == 0) return;

        user.setXpPoints(user.getXpPoints() + xpToAdd);
        userRepository.save(user);

        log.debug("Awarded {} XP to user '{}' for action '{}'. Total: {}",
                xpToAdd, user.getUsername(), action, user.getXpPoints());

        checkAndAwardBadges(user);
    }

    private boolean isFirstMessageToday(User user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return !chatMessageRepository.existsByUserIdAndSenderTypeAndCreatedAtAfter(
                user.getId(), "USER", startOfDay);
    }

    private boolean hadRecentCrisis(User user) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return chatMessageRepository
                .findTop20ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(m -> m.getCreatedAt() != null && m.getCreatedAt().isAfter(since))
                .anyMatch(m -> m.getIsCrisis() != null && m.getIsCrisis());
    }

    private boolean hasWeeklyMoodStreak(User user) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<MoodEntry> recent = moodEntryRepository
                .findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(
                        user.getId(), sevenDaysAgo);

        Set<LocalDate> activeDates = recent.stream()
                .map(e -> e.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());

        // Check if all 7 days are covered
        for (int i = 0; i < 7; i++) {
            if (!activeDates.contains(LocalDate.now().minusDays(i))) return false;
        }
        return true;
    }

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

    public int calculateStreak(User user) {
        List<MoodEntry> entries = moodEntryRepository
                .findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(
                        user.getId(), LocalDateTime.now().minusDays(365));

        if (entries.isEmpty()) return 0;

        Set<LocalDate> activeDates = entries.stream()
                .map(e -> e.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate date = LocalDate.now();
        while (activeDates.contains(date)) {
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }

    public List<Map<String, Object>> getUserBadges(User user) {
        return userBadgeRepository.findByUserOrderByEarnedAtDesc(user)
                .stream()
                .map(ub -> {
                    Map<String, Object> badgeMap = new LinkedHashMap<>();
                    badgeMap.put("id", ub.getBadge().getId());
                    badgeMap.put("name", ub.getBadge().getName());
                    badgeMap.put("description", ub.getBadge().getDescription());
                    String icon = ub.getBadge().getIcon();
                    if (icon == null || icon.equals("?")) {
                        icon = BADGE_ICONS.getOrDefault(ub.getBadge().getName(), "🏅");
                    }
                    badgeMap.put("icon", icon);
                    badgeMap.put("earnedAt", ub.getEarnedAt().toString());
                    return badgeMap;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getGamificationProfile(User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("xpStatus", getXpStatus(user));
        profile.put("streak", calculateStreak(user));
        profile.put("badges", getUserBadges(user));
        profile.put("badgeCount", getUserBadges(user).size());
        return profile;
    }

    @Transactional
    private void checkAndAwardBadges(User user) {
        int xp = user.getXpPoints() != null ? user.getXpPoints() : 0;
        List<String> alreadyEarned = userBadgeRepository
                .findByUserOrderByEarnedAtDesc(user)
                .stream()
                .map(ub -> ub.getBadge().getName())
                .collect(Collectors.toList());

        awardBadgeIfEligible(user, "First Steps",     xp >= 10,  alreadyEarned);
        awardBadgeIfEligible(user, "Getting Started", xp >= 50,  alreadyEarned);
        awardBadgeIfEligible(user, "Committed",       xp >= 100, alreadyEarned);
        awardBadgeIfEligible(user, "Dedicated",       xp >= 250, alreadyEarned);
        awardBadgeIfEligible(user, "Champion",        xp >= 500, alreadyEarned);

        int streak = calculateStreak(user);
        awardBadgeIfEligible(user, "3-Day Streak",    streak >= 3,  alreadyEarned);
        awardBadgeIfEligible(user, "Week Warrior",    streak >= 7,  alreadyEarned);
        awardBadgeIfEligible(user, "Monthly Master",  streak >= 30, alreadyEarned);
    }

    private static final Map<String, String> BADGE_ICONS = Map.of(
            "First Steps",     "🌱",
            "Getting Started", "🚀",
            "Committed",       "💪",
            "Dedicated",       "🏆",
            "Champion",        "👑",
            "3-Day Streak",    "🔥",
            "Week Warrior",    "⚡",
            "Monthly Master",  "🌟"
    );

    private void awardBadgeIfEligible(User user, String badgeName,
                                      boolean condition, List<String> alreadyEarned) {
        if (!condition || alreadyEarned.contains(badgeName)) return;
        badgeRepository.findByName(badgeName).ifPresent(badge -> {
            // Set icon if missing
            if (badge.getIcon() == null || badge.getIcon().equals("?")) {
                badge.setIcon(BADGE_ICONS.getOrDefault(badgeName, "🏅"));
                badgeRepository.save(badge);
            }
            UserBadge userBadge = UserBadge.builder()
                    .user(user).badge(badge)
                    .earnedAt(LocalDateTime.now()).build();
            userBadgeRepository.save(userBadge);
            log.info("Badge '{}' awarded to '{}'", badgeName, user.getUsername());
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