package com.mindcompanion.model;

import com.mindcompanion.model.enums.SentimentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // AI generated prompt that inspired this entry
    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    // User's journal content (stored encrypted)
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // Sentiment of this journal entry
    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment")
    private SentimentType sentiment;

    // AI generated summary/insight of the entry
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    // Tags auto-extracted by AI e.g. "anxiety,work,sleep"
    @Column(name = "tags")
    private String tags;

    // Date of the journal entry
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationship back to User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}