package com.mindcompanion.dto.response;

import com.mindcompanion.model.enums.MoodLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodResponse {

    private Long id;
    private Integer moodScore;
    private MoodLevel moodLevel;
    private String notes;
    private String aiInsight;
    private LocalDate entryDate;
    private LocalDateTime createdAt;

    // For weekly trend chart
    private String weeklyTrend;
    private Double averageMoodScore;
}