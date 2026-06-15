package com.tracker.MoneyTracker.goal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController() {
        this.goalService = null;
    }

    @Autowired
    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<SpendingGoal> createGoal(@RequestBody SpendingGoal goal) {
        if (goalService == null) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        SpendingGoal created = goalService.createGoal(goal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<SpendingGoal>> getGoals(@RequestParam("userId") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (goalService == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(goalService.getGoalsByUser(userId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<SpendingGoal>> getActiveGoals(@RequestParam("userId") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (goalService == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(goalService.getActiveGoalsByUser(userId));
    }

    @PutMapping("/{goalId}")
    public ResponseEntity<SpendingGoal> updateGoal(@PathVariable String goalId,
                                                    @RequestBody SpendingGoal goal) {
        if (goalService == null) {
            return ResponseEntity.ok(goal);
        }
        try {
            SpendingGoal updated = goalService.updateGoal(goalId, goal);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(@PathVariable String goalId) {
        if (goalService == null) {
            return ResponseEntity.noContent().build();
        }
        try {
            goalService.deleteGoal(goalId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/{goalId}/progress")
    public ResponseEntity<GoalProgress> getGoalProgress(@PathVariable String goalId) {
        if (goalService == null) {
            return ResponseEntity.ok(null);
        }
        try {
            GoalProgress progress = goalService.getGoalProgress(goalId);
            return ResponseEntity.ok(progress);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
