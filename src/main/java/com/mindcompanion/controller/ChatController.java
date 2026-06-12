package com.mindcompanion.controller;

import com.mindcompanion.dto.request.ChatRequest;
import com.mindcompanion.dto.response.ChatResponse;
import com.mindcompanion.model.ChatMessage;
import com.mindcompanion.repository.ChatMessageRepository;
import com.mindcompanion.repository.UserRepository;
import com.mindcompanion.service.ChatService;
import com.mindcompanion.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EncryptionUtil encryptionUtil;

    @PostMapping("/api/chat/send")
    public ResponseEntity<ChatResponse> sendMessageRest(
            @RequestBody ChatRequest request,
            Principal principal) {
        String username = principal.getName();
        log.debug("REST chat message from: {}", username);
        ChatResponse response = chatService.processMessage(request, username);
        return ResponseEntity.ok(response);
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatRequest request, Principal principal) {
        try {
            String username = principal.getName();
            ChatResponse response = chatService.processMessage(request, username);
            messagingTemplate.convertAndSendToUser(username, "/queue/messages", response);
            if (Boolean.TRUE.equals(response.getIsCrisis())) {
                messagingTemplate.convertAndSendToUser(username, "/queue/crisis", response);
            }
        } catch (Exception e) {
            log.error("Error processing WS message: {}", e.getMessage());
        }
    }

    @GetMapping("/api/chat/history")
    public List<ChatResponse> getChatHistory(Principal principal) {
        String username = principal.getName();
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        return chatMessageRepository
                .findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
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

    @DeleteMapping("/api/chat/history")
    public void deleteChatHistory(Principal principal) {
        String username = principal.getName();
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        chatMessageRepository.deleteAllByUserId(userId);
        log.info("Chat history deleted for user: {}", username);
    }
}