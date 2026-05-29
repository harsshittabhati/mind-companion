package com.mindcompanion.service;

import com.mindcompanion.dto.request.JournalRequest;
import com.mindcompanion.dto.response.JournalResponse;
import com.mindcompanion.model.JournalEntry;
import com.mindcompanion.model.User;
import com.mindcompanion.model.enums.SentimentType;
import com.mindcompanion.repository.JournalEntryRepository;
import com.mindcompanion.repository.UserRepository;
import com.mindcompanion.util.EncryptionUtil;
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
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    // ─── Save journal entry ──────────────────────────
    @Transactional
    public JournalResponse saveJournalEntry(JournalRequest request,
                                            String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + username));

        LocalDate today = LocalDate.now();
        boolean alreadyJournaled = journalEntryRepository
                .existsByUserIdAndEntryDate(user.getId(), today);

        // Analyze sentiment of journal content
        SentimentType sentiment = analyzeSentiment(request.getContent());

        // Generate AI summary and tags
        String aiSummary = generateSummary(
                request.getContent(), sentiment);
        String tags = extractTags(request.getContent());

        if (alreadyJournaled) {
            // Update existing entry
            JournalEntry existing = journalEntryRepository
                    .findByUserIdAndEntryDate(user.getId(), today)
                    .orElseThrow();
            existing.setContent(
                    encryptionUtil.encrypt(request.getContent()));
            existing.setSentiment(sentiment);
            existing.setAiSummary(aiSummary);
            existing.setTags(tags);
            JournalEntry saved = journalEntryRepository.save(existing);
            log.debug("Updated journal entry for user: {}", username);
            return buildResponse(saved, user.getId(), true);
        }

        // Create new entry
        JournalEntry entry = JournalEntry.builder()
                .content(encryptionUtil.encrypt(request.getContent()))
                .prompt(request.getPrompt())
                .sentiment(sentiment)
                .aiSummary(aiSummary)
                .tags(tags)
                .entryDate(today)
                .user(user)
                .build();

        JournalEntry saved = journalEntryRepository.save(entry);
        log.debug("Saved journal entry for user: {}", username);

        // Update user streak
        updateStreak(user);

        return buildResponse(saved, user.getId(), false);
    }

    // ─── Get today's journal entry ───────────────────
    public JournalResponse getTodayEntry(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + username));

        return journalEntryRepository
                .findByUserIdAndEntryDate(
                        user.getId(), LocalDate.now())
                .map(entry -> buildResponse(
                        entry, user.getId(), true))
                .orElse(null);
    }

    // ─── Get journal history ─────────────────────────
    public List<JournalResponse> getJournalHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + username));

        return journalEntryRepository
                .findByUserIdOrderByEntryDateDesc(user.getId())
                .stream()
                .map(entry -> buildResponse(
                        entry, user.getId(), true))
                .collect(Collectors.toList());
    }

    // ─── Get today's AI journal prompt ──────────────
    public String getTodayPrompt(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + username));

        Long totalEntries = journalEntryRepository
                .countByUserId(user.getId());

        // Rotate through prompts based on entry count
        List<String> prompts = List.of(
                "What are three things you're grateful for today?",
                "Describe a moment today that made you smile.",
                "What's one challenge you faced today and how did you handle it?",
                "How did your body feel today? Where did you hold tension?",
                "What emotion showed up most for you today?",
                "What would you tell your past self from one year ago?",
                "What boundaries did you set or need to set today?",
                "Describe your ideal version of tomorrow.",
                "What drained your energy today? What gave you energy?",
                "Write about something you're proud of this week."
        );

        int index = (int) (totalEntries % prompts.size());
        return prompts.get(index);
    }

    // ─── Sentiment analysis ──────────────────────────
    private SentimentType analyzeSentiment(String content) {
        String lower = content.toLowerCase();

        long negativeCount = List.of(
                "sad", "anxious", "depressed", "stressed", "worried",
                "angry", "frustrated", "terrible", "awful", "lonely",
                "tired", "exhausted", "hopeless", "overwhelmed", "scared"
        ).stream().filter(lower::contains).count();

        long positiveCount = List.of(
                "happy", "grateful", "excited", "peaceful", "calm",
                "proud", "hopeful", "joyful", "blessed", "thankful",
                "wonderful", "amazing", "great", "better", "confident"
        ).stream().filter(lower::contains).count();

        if (negativeCount > positiveCount) return SentimentType.NEGATIVE;
        if (positiveCount > negativeCount) return SentimentType.POSITIVE;
        return SentimentType.NEUTRAL;
    }

    // ─── Generate AI summary ─────────────────────────
    private String generateSummary(String content,
                                   SentimentType sentiment) {
        int wordCount = content.split("\\s+").length;

        return switch (sentiment) {
            case POSITIVE -> "Your entry reflects a positive mindset. " +
                    "You expressed " + wordCount + " words of reflection.";
            case NEGATIVE -> "Your entry shows you're processing " +
                    "some difficult emotions. That takes courage. " +
                    wordCount + " words written.";
            case CRISIS -> "Your entry contains some concerning themes. " +
                    "Please consider reaching out for support.";
            default -> "A thoughtful reflection of " +
                    wordCount + " words.";
        };
    }

    // ─── Extract tags from content ───────────────────
    private String extractTags(String content) {
        String lower = content.toLowerCase();

        List<String> possibleTags = List.of(
                "work", "family", "relationship", "health", "sleep",
                "anxiety", "depression", "stress", "gratitude", "exercise",
                "meditation", "food", "friends", "money", "future",
                "past", "self-care", "anger", "fear", "joy"
        );

        return possibleTags.stream()
                .filter(lower::contains)
                .collect(Collectors.joining(","));
    }

    // ─── Update user streak ──────────────────────────
    @Transactional
    private void updateStreak(User user) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        boolean journaledYesterday = journalEntryRepository
                .existsByUserIdAndEntryDate(user.getId(), yesterday);

        if (journaledYesterday) {
            user.setCurrentStreak(user.getCurrentStreak() + 1);
        } else {
            user.setCurrentStreak(1);
        }

        if (user.getCurrentStreak() > user.getLongestStreak()) {
            user.setLongestStreak(user.getCurrentStreak());
        }

        userRepository.save(user);
    }

    // ─── Build JournalResponse ───────────────────────
    private JournalResponse buildResponse(JournalEntry entry,
                                          Long userId,
                                          boolean decrypt) {
        Long totalEntries = journalEntryRepository.countByUserId(userId);
        boolean journaledToday = journalEntryRepository
                .existsByUserIdAndEntryDate(userId, LocalDate.now());

        String content = decrypt
                ? encryptionUtil.decrypt(entry.getContent())
                : entry.getContent();

        return JournalResponse.builder()
                .id(entry.getId())
                .content(content)
                .prompt(entry.getPrompt())
                .sentiment(entry.getSentiment())
                .aiSummary(entry.getAiSummary())
                .tags(entry.getTags())
                .entryDate(entry.getEntryDate())
                .createdAt(entry.getCreatedAt())
                .totalEntries(totalEntries)
                .alreadyJournaledToday(journaledToday)
                .build();
    }
}