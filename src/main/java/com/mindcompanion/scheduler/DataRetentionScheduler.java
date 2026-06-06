package com.mindcompanion.scheduler;

import com.mindcompanion.model.User;
import com.mindcompanion.repository.ChatMessageRepository;
import com.mindcompanion.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataRetentionScheduler {

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runRetentionCleanup() {
        log.info("Running GDPR data retention cleanup...");

        List<User> users = userRepository.findAll();
        int totalDeleted = 0;

        for (User user : users) {
            Integer days = user.getDataRetentionDays();
            if (days == null || days <= 0) continue;

            LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
            int deleted = chatMessageRepository
                    .deleteByUserIdAndCreatedAtBefore(user.getId(), cutoff);

            if (deleted > 0) {
                log.info("Deleted {} messages for user {} (retention: {} days)",
                        deleted, user.getId(), days);
                totalDeleted += deleted;
            }
        }

        log.info("Retention cleanup complete. Total messages deleted: {}", totalDeleted);
    }
}