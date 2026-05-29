package com.mindcompanion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JournalRequest {

    // The journal content written by user
    @NotBlank(message = "Journal content cannot be empty")
    @Size(min = 10, max = 5000,
            message = "Journal must be between 10 and 5000 characters")
    private String content;

    // Optional: the AI prompt that inspired this entry
    private String prompt;
}