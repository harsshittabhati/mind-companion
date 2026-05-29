package com.mindcompanion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 2000, message = "Message too long")
    private String message;

    // Session ID groups messages into one conversation
    // Frontend generates this as a UUID when chat opens
    private String sessionId;
}