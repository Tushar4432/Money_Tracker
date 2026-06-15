package com.tracker.MoneyTracker.goal;

import com.tracker.MoneyTracker.error.ErrorCode;
import com.tracker.MoneyTracker.exception.BadRequestException;
import com.tracker.MoneyTracker.exception.ResourceNotFoundException;
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

    public SpendingGoal createGoal(SpendingGoal goal) {
        validateGoal(goal);
        if (goal.getId() == null || goal.getId().isBlank()) {
            goal.setId(UUID.randomUUID().toString());
        }
        goal.setCreatedAt(LocalDate.now().atStartOfDay());
        goal.setUpdatedAt(LocalDate.now().atStartOfDay());
        return goalRepository.save(goal);
    }

    public List<SpendingGoal> getGoalsByUser(String userId) {
        if (goalRepository == null || userId == null || userId.isBlank()) {
            return List.of();
        }
        return goalRepository.findByUserId(userId);
    }

    public List<SpendingGoal> getActiveGoalsByUser(String userId) {
        if (goalRepository == null || userId == null || userId.isBlank()) {
            return List.of();
        }
        return goalRepository.findByUserIdAndActive(userId, true);
    }

    public SpendingGoal updateGoal(String goalId, SpendingGoal update) {
        SpendingGoal existing = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.GOAL_NOT_FOUND, goalId));

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

    public void deleteGoal(String goalId) {
        SpendingGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.GOAL_NOT_FOUND, goalId));
        goalRepository.delete(goal);
    }

    public GoalProgress getGoalProgress(String goalId) {
        SpendingGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.GOAL_NOT_FOUND, goalId));

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
            throw new BadRequestException(ErrorCode.VALIDATION_ERROR, "targetAmount must be positive");
        }
        if (goal.getCategory() == null || goal.getCategory().isBlank()) {
            throw new BadRequestException(ErrorCode.MISSING_FIELD, "category");
        }
        if (goal.getPeriod() == null || !VALID_PERIODS.contains(goal.getPeriod())) {
            throw new BadRequestException(ErrorCode.VALIDATION_ERROR,
                    "period must be one of: " + VALID_PERIODS);
        }
    }

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
            } catch (ResourceNotFoundException ignored) {
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
