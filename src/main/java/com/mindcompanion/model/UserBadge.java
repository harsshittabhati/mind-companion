package com.mindcompanion.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_badges")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // When the badge was earned
    @Column(name = "earned_at")
    private LocalDateTime earnedAt;

    // Relationship to User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Relationship to Badge
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @PrePersist
    protected void onCreate() {
        earnedAt = LocalDateTime.now();
    }
}