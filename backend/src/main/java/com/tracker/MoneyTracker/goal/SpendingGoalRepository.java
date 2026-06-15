package com.tracker.MoneyTracker.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpendingGoalRepository extends JpaRepository<SpendingGoal, String> {
    List<SpendingGoal> findByUserId(String userId);
    List<SpendingGoal> findByUserIdAndActive(String userId, boolean active);
    List<SpendingGoal> findByUserIdAndCategory(String userId, String category);
}
