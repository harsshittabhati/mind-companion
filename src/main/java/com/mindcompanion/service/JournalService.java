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
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final GamificationService gamificationService;
    private final Random random = new Random();

    @Transactional
    public JournalResponse saveJournalEntry(JournalRequest request, String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        SentimentType sentiment = analyzeSentiment(request.getContent());
        String aiSummary = generateSummary(request.getContent(), sentiment);
        String tags = extractTags(request.getContent());

        // Always create a new entry — allow multiple per day
        JournalEntry entry = JournalEntry.builder()
                .title(request.getTitle())
                .content(encryptionUtil.encrypt(request.getContent()))
                .prompt(request.getPrompt())
                .sentiment(sentiment)
                .aiSummary(aiSummary)
                .tags(tags)
                .entryDate(LocalDate.now())
                .user(user)
                .build();

        JournalEntry saved = journalEntryRepository.save(entry);
        int wordCount = request.getContent().trim().isEmpty() ? 0
                : request.getContent().trim().split("\\s+").length;
        gamificationService.awardXp(user, "JOURNAL_ENTRY", null, wordCount);
        log.debug("Saved journal entry for user: {}", username);

        updateStreak(user);

        return buildResponse(saved, user.getId(), false);
    }

    public JournalResponse getTodayEntry(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return journalEntryRepository
                .findByUserIdAndEntryDate(user.getId(), LocalDate.now())
                .map(entry -> buildResponse(entry, user.getId(), true))
                .orElse(null);
    }

    public List<JournalResponse> getJournalHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return journalEntryRepository
                .findByUserIdOrderByEntryDateDesc(user.getId())
                .stream()
                .map(entry -> buildResponse(entry, user.getId(), true))
                .collect(Collectors.toList());
    }

    public String getTodayPrompt(String username) {
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
                "Write about something you're proud of this week.",
                "What's one thing you wish someone knew about how you're feeling?",
                "If today were a color, what would it be and why?",
                "What does your inner critic say most often? Is it true?",
                "When did you last feel truly at peace? What were you doing?",
                "What small act of kindness did you give or receive today?"
        );

        return "{\"prompt\": \"" + prompts.get(random.nextInt(prompts.size())) + "\"}";
    }

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

    private String generateSummary(String content, SentimentType sentiment) {
        int wordCount = content.split("\\s+").length;
        return switch (sentiment) {
            case POSITIVE -> "Your entry reflects a positive mindset. You expressed " + wordCount + " words of reflection.";
            case NEGATIVE -> "Your entry shows you're processing some difficult emotions. That takes courage. " + wordCount + " words written.";
            case CRISIS -> "Your entry contains some concerning themes. Please consider reaching out for support.";
            default -> "A thoughtful reflection of " + wordCount + " words.";
        };
    }

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

    private JournalResponse buildResponse(JournalEntry entry, Long userId, boolean decrypt) {
        Long totalEntries = journalEntryRepository.countByUserId(userId);
        boolean journaledToday = journalEntryRepository
                .existsByUserIdAndEntryDate(userId, LocalDate.now());

        String content = decrypt
                ? encryptionUtil.decrypt(entry.getContent())
                : entry.getContent();

        return JournalResponse.builder()
                .id(entry.getId())
                .title(entry.getTitle())
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