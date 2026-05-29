package com.mindcompanion.repository;

import com.mindcompanion.model.EmergencyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyAlertRepository
        extends JpaRepository<EmergencyAlert, Long> {

    // Get all alerts for a user
    List<EmergencyAlert> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Get unresolved alerts
    List<EmergencyAlert> findByIsResolvedFalseOrderByCreatedAtDesc();

    // Count alerts for a user
    Long countByUserId(Long userId);
}