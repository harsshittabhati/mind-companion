package com.mindcompanion.model;

import com.mindcompanion.model.enums.SentimentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who sent it: "USER" or "BOT"
    @Column(name = "sender_type", nullable = false)
    private String senderType;

    // Stored encrypted (AES-256)
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // Sentiment analysis result
    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment")
    private SentimentType sentiment;

    // Emotional intensity 0.0 → 1.0
    @Column(name = "intensity_score")
    private Double intensityScore;

    // Was this message flagged as crisis?
    @Column(name = "is_crisis")
    @Builder.Default
    private Boolean isCrisis = false;

    // Session grouping (each chat session gets a UUID)
    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Relationship back to User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}