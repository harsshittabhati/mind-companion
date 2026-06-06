package com.mindcompanion.controller;

import com.mindcompanion.dto.response.MessageResponse;
import com.mindcompanion.model.User;
import com.mindcompanion.repository.*;
import com.mindcompanion.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserDataController {

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MoodEntryRepository moodEntryRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final EmergencyAlertRepository emergencyAlertRepository;
    private final UserBadgeRepository userBadgeRepository;

    @GetMapping("/privacy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPrivacySettings(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(new PrivacySettingsResponse(
                user.getDataRetentionDays(),
                Boolean.TRUE.equals(user.getConfidentialMode())
        ));
    }

    @PutMapping("/privacy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updatePrivacySettings(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody PrivacySettingsRequest request) {

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getRetentionDays() != null) {
            user.setDataRetentionDays(request.getRetentionDays());
        }
        if (request.getConfidentialMode() != null) {
            user.setConfidentialMode(request.getConfidentialMode());
        }

        userRepository.save(user);
        log.info("Privacy settings updated for user {}", userDetails.getId());

        return ResponseEntity.ok(new MessageResponse("Privacy settings updated successfully"));
    }

    @DeleteMapping("/data")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> eraseAllData(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();
        log.warn("GDPR erasure requested for user {}", userId);

        chatMessageRepository.deleteAllByUserId(userId);
        moodEntryRepository.deleteAllByUserId(userId);
        journalEntryRepository.deleteAllByUserId(userId);
        emergencyAlertRepository.deleteAllByUserId(userId);
        userBadgeRepository.deleteAllByUserId(userId);

        log.warn("All data erased for user {}", userId);
        return ResponseEntity.ok(new MessageResponse("All your data has been permanently deleted."));
    }

    @DeleteMapping("/account")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();
        log.warn("Account deletion requested for user {}", userId);

        chatMessageRepository.deleteAllByUserId(userId);
        moodEntryRepository.deleteAllByUserId(userId);
        journalEntryRepository.deleteAllByUserId(userId);
        emergencyAlertRepository.deleteAllByUserId(userId);
        userBadgeRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);

        log.warn("Account fully deleted for user {}", userId);
        return ResponseEntity.ok(new MessageResponse("Your account and all associated data have been permanently deleted."));
    }

    record PrivacySettingsResponse(Integer retentionDays, boolean confidentialMode) {}

    static class PrivacySettingsRequest {
        private Integer retentionDays;
        private Boolean confidentialMode;
        public Integer getRetentionDays() { return retentionDays; }
        public Boolean getConfidentialMode() { return confidentialMode; }
    }
}