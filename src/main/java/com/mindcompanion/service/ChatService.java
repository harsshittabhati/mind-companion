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
            "want to kill", "plan to end", "going to end my life",
            "don't want to be alive", "rather be dead"
    );

    private static final String SYSTEM_PROMPT = """
        You are a warm, compassionate AI mental health companion named "Serenity".
        Your role is to provide emotional support, active listening, and helpful
        coping strategies.

        Important guidelines:
        - For casual greetings like "hello", "hi", "hey" — respond warmly and
          naturally, like a friendly companion would. Keep it brief and inviting.
        - For emotional topics — ALWAYS validate feelings before offering advice.
          Never jump to solutions without first acknowledging how the person feels.
        - Use evidence-based techniques from CBT and mindfulness when appropriate.
        - Never diagnose or replace professional therapy.
            - Format responses for easy reading. Always use actual line breaks between thoughts.
                          * Each sentence or thought on its own line
                          * For tips or steps, put each bullet on a new line starting with •
                          * Never combine multiple points into one paragraph
                          * Casual messages: 2-3 lines max
                          * Emotional messages: 4-6 lines with a blank line between sections
                          * Example format for tips:
                            Here are some things that might help:
            
                            • Practice explaining to a friend
                            • Record yourself and watch it back
                            • Break complex ideas into simple words
            
                            Which of these feels most doable for you?
        - This conversation is completely private and confidential.

        CRISIS RESPONSE PROTOCOL — follow this order strictly:
        1. Lead with empathy and acknowledgment. Make the person feel heard first.
           Example: "I hear you, and I'm really glad you're talking to me right now."
        2. Gently express care for their safety without being clinical or alarming.
           Example: "What you're feeling matters deeply, and so does your life."
        3. Only after the above, naturally and warmly mention that support is available.
           Example: "If things feel overwhelming, reaching out to someone can help —
           iCall (9152987821) and AASRA (9820466627) have people ready to listen."
        4. Never lead with a list of phone numbers. Never sound robotic or scripted.
        5. End with an open question to keep the conversation going.
           Example: "Can you tell me more about what's been happening for you?"

        Crisis resources to weave in naturally when needed:
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

        boolean isFirstMessageOfSession = chatMessageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId).isEmpty();

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
        String aiReply = getAiResponse(userMessage, user, sessionId);
        boolean aiFailed = (aiReply == null);
        if (aiFailed) {
            aiReply = "I'm having trouble connecting right now. Please try again in a moment.";
        }

        // 5. Save AI response (encrypted) — skip if confidential mode is on, and skip saving error placeholders
        if (!confidential && !aiFailed) {
            ChatMessage botChatMessage = ChatMessage.builder()
                    .senderType("BOT")
                    .content(encryptionUtil.encrypt(aiReply))
                    .sessionId(sessionId)
                    .user(user)
                    .build();
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
                .aiError(aiFailed)
                .sessionTitle(isFirstMessageOfSession ? generateSessionTitle(userMessage) : null)
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

    public String generateJournalTitle(String content) {
        try {
            String snippet = content.length() > 300 ? content.substring(0, 300) : content;
            List<Object> messages = new ArrayList<>();
            messages.add(new java.util.HashMap<>() {{
                put("role", "system");
                put("content", "You generate short journal entry titles. Given the content of a journal entry, " +
                        "respond with ONLY a 3-6 word title that captures the main theme or emotion. " +
                        "No punctuation, no quotes, no explanation — just the title.");
            }});
            messages.add(new java.util.HashMap<>() {{
                put("role", "user");
                put("content", snippet);
            }});

            String requestBody = objectMapper.writeValueAsString(
                    new java.util.HashMap<>() {{
                        put("model", openAiModel);
                        put("messages", messages);
                        put("max_tokens", 16);
                        put("temperature", 0.3);
                    }}
            );

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response httpResponse = client.newCall(httpRequest).execute()) {
                if (httpResponse.isSuccessful() && httpResponse.body() != null) {
                    JsonNode jsonNode = objectMapper.readTree(httpResponse.body().string());
                    String title = jsonNode.path("choices").path(0).path("message").path("content").asText("").trim();
                    if (!title.isEmpty()) return title;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to generate journal title: {}", e.getMessage());
        }
        return null;
    }

    private String generateSessionTitle(String firstMessage) {
        try {
            List<Object> messages = new ArrayList<>();
            messages.add(new java.util.HashMap<>() {{
                put("role", "system");
                put("content", "You generate short chat titles. Given a user's first message to a " +
                        "mental wellness companion, respond with ONLY a 2-4 word title summarizing the " +
                        "topic or intent (e.g. 'Greeting', 'Work Stress', 'Sleep Trouble', 'Feeling Anxious'). " +
                        "No punctuation, no quotes, no explanation — just the title.");
            }});
            messages.add(new java.util.HashMap<>() {{
                put("role", "user");
                put("content", firstMessage);
            }});

            String requestBody = objectMapper.writeValueAsString(
                    new java.util.HashMap<>() {{
                        put("model", openAiModel);
                        put("messages", messages);
                        put("max_tokens", 12);
                        put("temperature", 0.3);
                    }}
            );

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            Request httpRequest = new Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();

            try (Response httpResponse = client.newCall(httpRequest).execute()) {
                if (httpResponse.isSuccessful() && httpResponse.body() != null) {
                    JsonNode jsonNode = objectMapper.readTree(httpResponse.body().string());
                    String title = jsonNode.path("choices").path(0).path("message").path("content").asText("").trim();
                    if (!title.isEmpty()) return title;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to generate session title: {}", e.getMessage());
        }
        return null;
    }

    private String getAiResponse(String userMessage, User user, String sessionId) {
        try {
            log.debug("Calling Groq API with model: {}", openAiModel);

            String sid = (sessionId != null) ? sessionId : "";
            List<ChatMessage> history = chatMessageRepository
                    .findContextMessages(
                            user.getId(),
                            sid,
                            LocalDateTime.now().minusHours(24),
                            org.springframework.data.domain.PageRequest.of(0, 20))
                    .getContent();

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

        return null;
    }
}