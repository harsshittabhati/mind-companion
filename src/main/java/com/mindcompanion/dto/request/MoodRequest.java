package com.mindcompanion.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoodRequest {

    // Mood score 1-10
    @NotNull(message = "Mood score is required")
    @Min(value = 1, message = "Mood score must be at least 1")
    @Max(value = 10, message = "Mood score must be at most 10")
    private Integer moodScore;

    // Optional note from user
    private String notes;
}