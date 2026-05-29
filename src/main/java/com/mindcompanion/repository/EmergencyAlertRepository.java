package com.mindcompanion.repository;

import com.mindcompanion.model.EmergencyAlert;
import com.mindcompanion.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {

    // All unresolved alerts (admin view)
    List<EmergencyAlert> findByIsResolvedFalse();

    // All alerts for a specific user, newest first
    List<EmergencyAlert> findByUserOrderByCreatedAtDesc(User user);

    // Alerts for a user where email hasn't been sent yet
    List<EmergencyAlert> findByUserAndEmailSentFalse(User user);
}