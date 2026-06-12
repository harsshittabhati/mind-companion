package com.mindcompanion.dto.response;

import com.mindcompanion.model.enums.SentimentType;
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
public class JournalResponse {

    private Long id;
    private String title;
    private String content;
    private String prompt;
    private SentimentType sentiment;
    private String aiSummary;
    private String tags;
    private LocalDate entryDate;
    private LocalDateTime createdAt;

    // Streak info
    private Long totalEntries;
    private Boolean alreadyJournaledToday;
}