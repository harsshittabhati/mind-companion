package com.mindcompanion.dto.response;

import com.mindcompanion.model.enums.SentimentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    // The AI's reply text
    private String message;

    // Who sent it: "USER" or "BOT"
    private String senderType;

    // Sentiment of the user's message
    private SentimentType sentiment;

    // Emotional intensity score 0.0 → 1.0
    private Double intensityScore;

    // Was this a crisis message?
    private Boolean isCrisis;

    // Session this message belongs to
    private String sessionId;

    // Timestamp
    private LocalDateTime createdAt;

    // If crisis detected — show helpline info
    private String emergencyMessage;
}