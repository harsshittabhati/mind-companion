package com.mindcompanion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcompanion.dto.request.ChatRequest;
import com.mindcompanion.dto.response.ChatResponse;
import com.mindcompanion.model.ChatMessage;
import com.mindcompanion.model.EmergencyAlert;
import com.mindcompanion.model.User;
import com.mindcompanion.model.enums.SentimentType;
import com.mindcompanion.repository.ChatMessageRepository;
import com.mindcompanion.repository.UserRepository;
import com.mindcompanion.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;
    private final EmergencyAlertService emergencyAlertService;
    private final EmailService emailService;
    private final GamificationService gamificationService;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.model}")
    private String openAiModel;

    @Value("${openai.max-tokens}")
    private int maxTokens;

    private static final Set<String> CRISIS_KEYWORDS = Set.of(
            "suicide", "kill myself", "end my life", "want to die",
            "can't go on", "no reason to live", "self harm", "hurt myself",
            "hopeless", "worthless", "give up", "can't take it anymore"
    );

    private static final String SYSTEM_PROMPT = """
        You are a warm and friendly AI mental health companion named "Serenity".
        Your role is to provide emotional support, active listening, and helpful
        coping strategies.

        Important guidelines:
        - For casual greetings like "hello", "hi", "hey" — respond warmly and
          naturally, like a friendly companion would. Keep it brief and inviting.
        - For emotional topics — validate feelings before offering advice
        - Use evidence-based techniques from CBT and mindfulness when appropriate
        - Never diagnose or replace professional therapy
        - If crisis language is detected, always encourage professional help
          and provide helpline numbers
        - Keep responses concise (2-4 sentences for casual, 3-5 for emotional)
        - This conversation is completely private and confidential

        Crisis resources to share when needed:
        - iCall (India): 9152987821
        - Vandrevala Foundation: 1860-2662-345
        - AASRA: 9820466627
        """;

    @Transactional
    public ChatResponse processMessage(ChatRequest request, String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        String userMessage = request.getMessage();
        String sessionId = request.getSessionId();

        // 1. Analyze sentiment
        SentimentType sentiment = analyzeSentiment(userMessage);
        double intensityScore = calculateIntensity(userMessage);
        boolean isCrisis = detectCrisis(userMessage);

        // 2. Save user message (encrypted) — skip if confidential mode is on
        boolean confidential = Boolean.TRUE.equals(user.getConfidentialMode());

        ChatMessage userChatMessage = ChatMessage.builder()
                .senderType("USER")
                .content(encryptionUtil.encrypt(userMessage))
                .sentiment(sentiment)
                .intensityScore(intensityScore)
                .isCrisis(isCrisis)
                .sessionId(sessionId)
                .user(user)
                .build();

        if (!confidential) {
            chatMessageRepository.save(userChatMessage);
        }

        // Award XP for message
        gamificationService.awardXp(user, "MESSAGE", sentiment, 0);

        // 3. Handle crisis — save alert + send email
        if (isCrisis) {
            handleCrisis(user, userMessage, intensityScore);
        }

        // 4. Get AI response from Groq
        String aiReply = getAiResponse(userMessage, user);

        // 5. Save AI response (encrypted) — skip if confidential mode is on
        ChatMessage botChatMessage = ChatMessage.builder()
                .senderType("BOT")
                .content(encryptionUtil.encrypt(aiReply))
                .sessionId(sessionId)
                .user(user)
                .build();

        if (!confidential) {
            chatMessageRepository.save(botChatMessage);
        }

        // 6. Build response
        ChatResponse response = ChatResponse.builder()
                .message(aiReply)
                .senderType("BOT")
                .sentiment(sentiment)
                .intensityScore(intensityScore)
                .isCrisis(isCrisis)
                .sessionId(sessionId)
                .createdAt(LocalDateTime.now())
                .build();

        // 7. Add emergency message if crisis detected
        if (isCrisis) {
            response.setEmergencyMessage(
                    "🚨 It sounds like you may be in crisis. " +
                            "Please reach out: iCall: 9152987821 | " +
                            "AASRA: 9820466627 | " +
                            "Vandrevala Foundation: 1860-2662-345"
            );
        }

        return response;
    }

    private void handleCrisis(User user, String userMessage, double intensityScore) {

        String triggeredKeyword = CRISIS_KEYWORDS.stream()
                .filter(userMessage.toLowerCase()::contains)
                .findFirst()
                .orElse("unknown");

        String triggerReason = "Crisis keyword detected in chat message";

        EmergencyAlert alert = emergencyAlertService.createAlert(
                user, triggerReason, triggeredKeyword, intensityScore);

        log.warn("🚨 Crisis detected for user='{}', keyword='{}', alertId={}",
                user.getUsername(), triggeredKeyword, alert.getId());

        try {
            emailService.sendCrisisAlertEmail(
                    user.getUsername(), triggeredKeyword, triggerReason);
            emergencyAlertService.markEmailSent(alert.getId());
        } catch (Exception e) {
            log.error("⚠️ Crisis alert email failed for user='{}', alertId={}: {}",
                    user.getUsername(), alert.getId(), e.getMessage());
        }
    }

    private SentimentType analyzeSentiment(String message) {
        String lower = message.toLowerCase();

        if (detectCrisis(message)) return SentimentType.CRISIS;

        long negativeCount = List.of(
                "sad", "depressed", "anxious", "worried", "scared",
                "angry", "frustrated", "upset", "terrible", "awful",
                "miserable", "lonely", "tired", "exhausted", "stressed"
        ).stream().filter(lower::contains).count();

        long positiveCount = List.of(
                "happy", "good", "great", "amazing", "wonderful",
                "excited", "grateful", "thankful", "better", "hopeful",
                "calm", "peaceful", "joyful", "proud", "confident"
        ).stream().filter(lower::contains).count();

        if (negativeCount > positiveCount) return SentimentType.NEGATIVE;
        if (positiveCount > negativeCount) return SentimentType.POSITIVE;
        return SentimentType.NEUTRAL;
    }

    private double calculateIntensity(String message) {
        String lower = message.toLowerCase();
        long matches = List.of(
                "very", "extremely", "really", "so much", "terribly",
                "incredibly", "absolutely", "completely", "totally",
                "always", "never", "nothing", "everything", "worst"
        ).stream().filter(lower::contains).count();
        return Math.min(matches * 0.15, 1.0);
    }

    private boolean detectCrisis(String message) {
        String lower = message.toLowerCase();
        return CRISIS_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private String getAiResponse(String userMessage, User user) {
        try {
            log.debug("Calling Groq API with model: {}", openAiModel);

            List<ChatMessage> history = chatMessageRepository
                    .findTop20ByUserIdOrderByCreatedAtDesc(user.getId());

            List<Object> messages = new ArrayList<>();

            messages.add(new java.util.HashMap<>() {{
                put("role", "system");
                put("content", SYSTEM_PROMPT);
            }});

            for (int i = history.size() - 1; i >= 0; i--) {
                ChatMessage msg = history.get(i);
                String role = msg.getSenderType().equals("USER") ? "user" : "assistant";
                String content = encryptionUtil.decrypt(msg.getContent());
                final String r = role;
                final String c = content;
                messages.add(new java.util.HashMap<>() {{
                    put("role", r);
                    put("content", c);
                }});
            }

            messages.add(new java.util.HashMap<>() {{
                put("role", "user");
                put("content", userMessage);
            }});

            String requestBody = objectMapper.writeValueAsString(
                    new java.util.HashMap<>() {{
                        put("model", openAiModel);
                        put("messages", messages);
                        put("max_tokens", maxTokens);
                        put("temperature", 0.7);
                    }}
            );

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response httpResponse = client.newCall(httpRequest).execute()) {
                String responseBodyStr = httpResponse.body() != null
                        ? httpResponse.body().string() : "empty body";

                if (httpResponse.isSuccessful()) {
                    JsonNode jsonNode = objectMapper.readTree(responseBodyStr);
                    String reply = jsonNode
                            .path("choices").path(0)
                            .path("message").path("content")
                            .asText("");

                    if (!reply.isEmpty()) return reply;
                    else log.error("Groq response had empty content. Full body: {}", responseBodyStr);
                } else {
                    log.error("Groq API failed. Code: {}, Body: {}", httpResponse.code(), responseBodyStr);
                }
            }

        } catch (Exception e) {
            log.error("Groq API error: {}", e.getMessage(), e);
        }

        return "I'm here for you. It sounds like you're going through " +
                "something difficult. Would you like to tell me more about how you're feeling?";
    }
}