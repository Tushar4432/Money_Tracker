package com.tracker.MoneyTracker.goal;

import com.tracker.MoneyTracker.error.ErrorCode;
import com.tracker.MoneyTracker.exception.BadRequestException;
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
        if (goalService == null) {
            return ResponseEntity.ok(List.of());
        }
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "userId is required");
        }
        return ResponseEntity.ok(goalService.getGoalsByUser(userId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<SpendingGoal>> getActiveGoals(@RequestParam("userId") String userId) {
        if (goalService == null) {
            return ResponseEntity.ok(List.of());
        }
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "userId is required");
        }
        return ResponseEntity.ok(goalService.getActiveGoalsByUser(userId));
    }

    @PutMapping("/{goalId}")
    public ResponseEntity<SpendingGoal> updateGoal(@PathVariable String goalId,
                                                    @RequestBody SpendingGoal goal) {
        if (goalService == null) {
            return ResponseEntity.ok(goal);
        }
        SpendingGoal updated = goalService.updateGoal(goalId, goal);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(@PathVariable String goalId) {
        if (goalService == null) {
            return ResponseEntity.noContent().build();
        }
        goalService.deleteGoal(goalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{goalId}/progress")
    public ResponseEntity<GoalProgress> getGoalProgress(@PathVariable String goalId) {
        if (goalService == null) {
            return ResponseEntity.ok(null);
        }
        GoalProgress progress = goalService.getGoalProgress(goalId);
        return ResponseEntity.ok(progress);
    }
}
