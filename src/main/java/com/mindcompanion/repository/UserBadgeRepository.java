package com.mindcompanion.repository;

import com.mindcompanion.model.User;
import com.mindcompanion.model.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUserOrderByEarnedAtDesc(User user);
}