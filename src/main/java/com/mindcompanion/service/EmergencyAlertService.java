package com.mindcompanion.service;

import com.mindcompanion.model.EmergencyAlert;
import com.mindcompanion.model.User;
import com.mindcompanion.repository.EmergencyAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyAlertService {

    private final EmergencyAlertRepository emergencyAlertRepository;

    /**
     * Creates and saves a crisis alert triggered by a specific keyword in a message.
     *
     * @param user           the user who sent the crisis message
     * @param triggerReason  human-readable reason e.g. "Crisis keyword detected in chat"
     * @param triggerKeyword the exact keyword that matched e.g. "suicide"
     * @param intensityScore sentiment intensity score at time of trigger (0.0 – 1.0)
     */
    public EmergencyAlert createAlert(User user,
                                      String triggerReason,
                                      String triggerKeyword,
                                      Double intensityScore) {

        EmergencyAlert alert = EmergencyAlert.builder()
                .user(user)
                .triggerReason(triggerReason)
                .triggerKeyword(triggerKeyword)
                .intensityScore(intensityScore)
                .emailSent(false)
                .smsSent(false)
                .isResolved(false)
                .build();

        EmergencyAlert saved = emergencyAlertRepository.save(alert);
        log.warn("🚨 Emergency alert [id={}] created for user='{}', keyword='{}', intensity={}",
                saved.getId(), user.getUsername(), triggerKeyword, intensityScore);
        return saved;
    }

    /**
     * Marks the emailSent flag as true after an alert email is dispatched.
     */
    public void markEmailSent(Long alertId) {
        emergencyAlertRepository.findById(alertId).ifPresent(alert -> {
            alert.setEmailSent(true);
            emergencyAlertRepository.save(alert);
            log.info("📧 Alert {} marked as email-sent.", alertId);
        });
    }

    /**
     * Resolves an alert with optional notes from admin/therapist.
     */
    public void resolveAlert(Long alertId, String resolutionNotes) {
        emergencyAlertRepository.findById(alertId).ifPresent(alert -> {
            alert.setIsResolved(true);
            alert.setResolvedAt(LocalDateTime.now());
            alert.setResolutionNotes(resolutionNotes);
            emergencyAlertRepository.save(alert);
            log.info("✅ Alert {} resolved. Notes: {}", alertId, resolutionNotes);
        });
    }

    /**
     * Returns all unresolved alerts — for admin/therapist dashboard.
     */
    public List<EmergencyAlert> getUnresolvedAlerts() {
        return emergencyAlertRepository.findByIsResolvedFalse();
    }

    /**
     * Returns full alert history for a specific user, newest first.
     */
    public List<EmergencyAlert> getAlertsForUser(User user) {
        return emergencyAlertRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Returns all alerts for a specific user that haven't had an email sent yet.
     */
    public List<EmergencyAlert> getUnsentAlertsForUser(User user) {
        return emergencyAlertRepository.findByUserAndEmailSentFalse(user);
    }
}