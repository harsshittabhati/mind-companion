package com.mindcompanion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JournalRequest {

    private String title;

    @NotBlank(message = "Journal content cannot be empty")
    @Size(min = 10, max = 5000,
            message = "Journal must be between 10 and 5000 characters")
    private String content;

    private String prompt;

    private String moodTag;
}