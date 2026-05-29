package com.mindcompanion.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "badges")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Badge name e.g. "First Session", "7-Day Streak"
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    // Description shown to user
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Icon name (maps to a CSS/image icon)
    @Column(name = "icon")
    private String icon;

    // XP reward for earning this badge
    @Column(name = "xp_reward")
    @Builder.Default
    private Integer xpReward = 0;
}