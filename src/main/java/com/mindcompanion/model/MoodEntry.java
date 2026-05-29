package com.mindcompanion.model;

import com.mindcompanion.model.enums.MoodLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mood_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Numeric score 1-10
    @Column(name = "mood_score", nullable = false)
    private Integer moodScore;

    // Mapped enum from score
    @Enumerated(EnumType.STRING)
    @Column(name = "mood_level", nullable = false)
    private MoodLevel moodLevel;

    // Optional note from user
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // AI generated insight for this entry
    @Column(name = "ai_insight", columnDefinition = "TEXT")
    private String aiInsight;

    // Date of the check-in (one per day)
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

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