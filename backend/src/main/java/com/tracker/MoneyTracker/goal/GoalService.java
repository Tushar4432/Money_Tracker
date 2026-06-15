package com.tracker.MoneyTracker.goal;

import com.tracker.MoneyTracker.transaction.Transaction;
import com.tracker.MoneyTracker.transaction.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class GoalService {

    private static final List<String> VALID_PERIODS = List.of("WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY");

    private final SpendingGoalRepository goalRepository;
    private final TransactionRepository transactionRepository;

    public GoalService() {
        this.goalRepository = null;
        this.transactionRepository = null;
    }

    @Autowired
    public GoalService(SpendingGoalRepository goalRepository, TransactionRepository transactionRepository) {
        this.goalRepository = goalRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Creates and saves a new spending goal after validating fields.
     */
    public SpendingGoal createGoal(SpendingGoal goal) {
        validateGoal(goal);
        if (goal.getId() == null || goal.getId().isBlank()) {
            goal.setId(UUID.randomUUID().toString());
        }
        goal.setCreatedAt(LocalDate.now().atStartOfDay());
        goal.setUpdatedAt(LocalDate.now().atStartOfDay());
        return goalRepository.save(goal);
    }

    /**
     * Returns all goals for a user.
     */
    public List<SpendingGoal> getGoalsByUser(String userId) {
        if (goalRepository == null || userId == null || userId.isBlank()) {
            return List.of();
        }
        return goalRepository.findByUserId(userId);
    }

    /**
     * Returns only active goals for a user.
     */
    public List<SpendingGoal> getActiveGoalsByUser(String userId) {
        if (goalRepository == null || userId == null || userId.isBlank()) {
            return List.of();
        }
        return goalRepository.findByUserIdAndActive(userId, true);
    }

    /**
     * Updates an existing goal. Only non-null fields from the update are applied.
     */
    public SpendingGoal updateGoal(String goalId, SpendingGoal update) {
        SpendingGoal existing = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + goalId));

        if (update.getCategory() != null && !update.getCategory().isBlank()) {
            existing.setCategory(update.getCategory());
        }
        if (update.getTargetAmount() != null && update.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            existing.setTargetAmount(update.getTargetAmount());
        }
        if (update.getPeriod() != null && VALID_PERIODS.contains(update.getPeriod())) {
            existing.setPeriod(update.getPeriod());
        }
        if (update.getStartDate() != null) {
            existing.setStartDate(update.getStartDate());
        }
        if (update.getEndDate() != null) {
            existing.setEndDate(update.getEndDate());
        }
        existing.setActive(update.isActive());
        existing.setUpdatedAt(LocalDate.now().atStartOfDay());

        return goalRepository.save(existing);
    }

    /**
     * Deletes a goal by ID.
     */
    public void deleteGoal(String goalId) {
        SpendingGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + goalId));
        goalRepository.delete(goal);
    }

    /**
     * Calculates goal progress by comparing actual spending against the target.
     */
    public GoalProgress getGoalProgress(String goalId) {
        SpendingGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + goalId));

        BigDecimal spent = calculateSpent(goal);
        BigDecimal remaining = goal.getTargetAmount().subtract(spent);
        double percentage = BigDecimal.ZERO.compareTo(goal.getTargetAmount()) < 0
                ? spent.multiply(BigDecimal.valueOf(100))
                    .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
                    .doubleValue()
                : 0.0;

        return new GoalProgress(
                goal.getId(),
                goal.getCategory(),
                goal.getTargetAmount(),
                spent,
                remaining,
                percentage,
                goal.getPeriod()
        );
    }

    private void validateGoal(SpendingGoal goal) {
        Objects.requireNonNull(goal, "Goal must not be null");
        if (goal.getTargetAmount() == null || goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("targetAmount must be positive");
        }
        if (goal.getCategory() == null || goal.getCategory().isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
        if (goal.getPeriod() == null || !VALID_PERIODS.contains(goal.getPeriod())) {
            throw new IllegalArgumentException("period must be one of: " + VALID_PERIODS);
        }
    }

    /**
     * Evaluates all active goals for a user and returns those needing attention.
     * A goal needs attention if spending has reached ≥80% of the target.
     *
     * @param userId the user ID
     * @return list of GoalProgress for goals at or above 80% usage
     */
    public List<GoalProgress> evaluateAllGoals(String userId) {
        if (goalRepository == null || userId == null || userId.isBlank()) {
            return List.of();
        }
        List<SpendingGoal> activeGoals = goalRepository.findByUserIdAndActive(userId, true);
        if (activeGoals.isEmpty()) {
            return List.of();
        }

        List<GoalProgress> atRisk = new ArrayList<>();
        for (SpendingGoal goal : activeGoals) {
            try {
                GoalProgress progress = getGoalProgress(goal.getId());
                if (progress.percentageUsed() >= 80.0) {
                    atRisk.add(progress);
                }
            } catch (IllegalArgumentException ignored) {
                // goal was deleted between fetch and evaluation, skip
            }
        }
        return atRisk;
    }

    private BigDecimal calculateSpent(SpendingGoal goal) {
        if (transactionRepository == null || goal.getUserId() == null) {
            return BigDecimal.ZERO;
        }
        LocalDate start = goal.getStartDate() != null ? goal.getStartDate() : LocalDate.now().withDayOfMonth(1);
        LocalDate end = goal.getEndDate() != null ? goal.getEndDate() : LocalDate.now();

        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetween(goal.getUserId(), start, end);

        return transactions.stream()
                .filter(t -> "DEBIT".equals(t.getType()))
                .filter(t -> goal.getCategory().equals(t.getCategory()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
