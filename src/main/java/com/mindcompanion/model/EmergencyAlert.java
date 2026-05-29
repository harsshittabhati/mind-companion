package com.mindcompanion.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // What triggered the alert e.g. "crisis keyword detected"
    @Column(name = "trigger_reason", nullable = false, columnDefinition = "TEXT")
    private String triggerReason;

    // The keyword or phrase that triggered it
    @Column(name = "trigger_keyword")
    private String triggerKeyword;

    // Sentiment intensity score at time of trigger
    @Column(name = "intensity_score")
    private Double intensityScore;

    // Was email alert sent to emergency contact?
    @Column(name = "email_sent")
    @Builder.Default
    private Boolean emailSent = false;

    // Was SMS sent?
    @Column(name = "sms_sent")
    @Builder.Default
    private Boolean smsSent = false;

    // Was the alert resolved/acknowledged?
    @Column(name = "is_resolved")
    @Builder.Default
    private Boolean isResolved = false;

    // Admin/therapist notes on this alert
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // Relationship back to User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}