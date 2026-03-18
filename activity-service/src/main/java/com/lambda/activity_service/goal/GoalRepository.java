package com.lambda.activity_service.activitymodule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserIdAndActiveTrue(Long userId);
}
