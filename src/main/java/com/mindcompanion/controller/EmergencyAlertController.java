package com.mindcompanion.controller;

import com.mindcompanion.model.EmergencyAlert;
import com.mindcompanion.model.User;
import com.mindcompanion.repository.UserRepository;
import com.mindcompanion.service.EmergencyAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class EmergencyAlertController {

    private final EmergencyAlertService emergencyAlertService;
    private final UserRepository userRepository;

    /**
     * GET /api/alerts/my
     * Returns all alerts for the currently logged-in user.
     */
    @GetMapping("/my")
    public ResponseEntity<List<EmergencyAlert>> getMyAlerts(
            Authentication authentication) {

        User user = getUser(authentication);
        List<EmergencyAlert> alerts =
                emergencyAlertService.getAlertsForUser(user);
        return ResponseEntity.ok(alerts);
    }

    /**
     * GET /api/alerts/unresolved
     * Returns all unresolved alerts across all users.
     * Intended for admin/therapist use.
     */
    @GetMapping("/unresolved")
    public ResponseEntity<List<EmergencyAlert>> getUnresolvedAlerts() {
        List<EmergencyAlert> alerts =
                emergencyAlertService.getUnresolvedAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * PUT /api/alerts/{id}/resolve
     * Marks an alert as resolved with optional notes.
     * Body: { "notes": "Checked in with user, situation stable." }
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<Map<String, String>> resolveAlert(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {

        String notes = (body != null)
                ? body.getOrDefault("notes", "") : "";

        emergencyAlertService.resolveAlert(id, notes);

        return ResponseEntity.ok(Map.of(
                "message", "Alert " + id + " resolved successfully.",
                "alertId", String.valueOf(id)
        ));
    }

    /**
     * GET /api/alerts/count/unresolved
     * Returns count of unresolved alerts — useful for dashboard badge.
     */
    @GetMapping("/count/unresolved")
    public ResponseEntity<Map<String, Integer>> getUnresolvedCount() {
        int count = emergencyAlertService.getUnresolvedAlerts().size();
        return ResponseEntity.ok(Map.of("unresolvedCount", count));
    }

    // ─── Helper ──────────────────────────────────────
    private User getUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found: " + username));
    }
}