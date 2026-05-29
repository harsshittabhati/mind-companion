package com.mindcompanion.controller;

import com.mindcompanion.dto.request.ChatRequest;
import com.mindcompanion.dto.response.ChatResponse;
import com.mindcompanion.model.ChatMessage;
import com.mindcompanion.repository.ChatMessageRepository;
import com.mindcompanion.service.ChatService;
import com.mindcompanion.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EncryptionUtil encryptionUtil;

    // ─── WebSocket endpoint ──────────────────────────
    @MessageMapping("/chat.send")
    public void sendMessage(
            @Payload ChatRequest request,
            Principal principal) {

        try {
            String username = principal.getName();
            log.debug("Message received from: {}", username);

            ChatResponse response = chatService
                    .processMessage(request, username);

            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/messages",
                    response
            );

            if (Boolean.TRUE.equals(response.getIsCrisis())) {
                messagingTemplate.convertAndSendToUser(
                        username,
                        "/queue/crisis",
                        response
                );
                log.warn("Crisis detected for user: {}", username);
            }

        } catch (Exception e) {
            log.error("Error processing message: {}", e.getMessage());
        }
    }

    // ─── REST endpoint: get chat history ────────────
    @GetMapping("/api/chat/history")
    public List<ChatResponse> getChatHistory(Principal principal) {

        String username = principal.getName();

        List<ChatMessage> messages = chatMessageRepository
                .findByUserIdOrderByCreatedAtAsc(
                        getUserIdByUsername(username));

        return messages.stream()
                .map(msg -> ChatResponse.builder()
                        .message(encryptionUtil.decrypt(msg.getContent()))
                        .senderType(msg.getSenderType())
                        .sentiment(msg.getSentiment())
                        .intensityScore(msg.getIntensityScore())
                        .isCrisis(msg.getIsCrisis())
                        .sessionId(msg.getSessionId())
                        .createdAt(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ─── REST endpoint: delete chat history ─────────
    @DeleteMapping("/api/chat/history")
    public void deleteChatHistory(Principal principal) {
        String username = principal.getName();
        chatMessageRepository.deleteAllByUserId(
                getUserIdByUsername(username));
        log.info("Chat history deleted for user: {}", username);
    }

    // ─── Helper ──────────────────────────────────────
    private Long getUserIdByUsername(String username) {
        return chatMessageRepository
                .findTop20ByUserIdOrderByCreatedAtDesc(1L)
                .stream()
                .findFirst()
                .map(msg -> msg.getUser().getId())
                .orElse(1L);
    }
}