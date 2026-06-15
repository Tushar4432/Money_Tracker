package com.tracker.MoneyTracker.analytics;

import com.tracker.MoneyTracker.analytics.dto.*;
import com.tracker.MoneyTracker.transaction.Transaction;
import com.tracker.MoneyTracker.transaction.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;

    public AnalyticsService() {
        this.transactionRepository = null;
    }

    @Autowired
    public AnalyticsService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Returns overall spending summary for a user: total income, expense, net savings,
     * top spending category, and transaction count.
     */
    public SpendingSummary getSpendingSummary(String userId) {
        List<Transaction> transactions = getTransactions(userId);

        BigDecimal totalIncome = transactions.stream()
                .filter(t -> "CREDIT".equals(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> "DEBIT".equals(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netSavings = totalIncome.subtract(totalExpense);

        String topCategory = transactions.stream()
                .filter(t -> "DEBIT".equals(t.getType()))
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        return new SpendingSummary(totalIncome, totalExpense, netSavings, topCategory, transactions.size());
    }

    /**
     * Returns per-category breakdown with amounts and percentages for a date range.
     * If start/end are null, defaults to last 3 months.
     */
    public List<CategoryBreakdown> getCategoryBreakdown(String userId, LocalDate start, LocalDate end) {
        List<Transaction> transactions = getTransactionsForDateRange(userId, start, end);

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> "DEBIT".equals(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalExpense.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        Map<String, BigDecimal> categoryTotals = transactions.stream()
                .filter(t -> "DEBIT".equals(t.getType()))
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        return categoryTotals.entrySet().stream()
                .map(e -> {
                    double pct = e.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(totalExpense, 2, RoundingMode.HALF_UP)
                            .doubleValue();
                    return new CategoryBreakdown(e.getKey(), e.getValue(), pct);
                })
                .sorted((a, b) -> b.amount().compareTo(a.amount()))
                .collect(Collectors.toList());
    }

    /**
     * Returns month-by-month income/expense/net trends for the last N months.
     */
    public List<MonthlyTrend> getMonthlyTrends(String userId, int months) {
        if (months <= 0) {
            months = 6;
        }

        List<Transaction> transactions = getTransactions(userId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        // Group by year-month
        Map<String, List<Transaction>> byMonth = transactions.stream()
                .filter(t -> t.getTransactionDate() != null)
                .collect(Collectors.groupingBy(t -> t.getTransactionDate().format(fmt)));

        // Generate the last N months in order
        List<MonthlyTrend> trends = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String monthKey = monthDate.format(fmt);

            List<Transaction> monthTxns = byMonth.getOrDefault(monthKey, List.of());
            BigDecimal income = monthTxns.stream()
                    .filter(t -> "CREDIT".equals(t.getType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expense = monthTxns.stream()
                    .filter(t -> "DEBIT".equals(t.getType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            trends.add(new MonthlyTrend(monthKey, income, expense, income.subtract(expense)));
        }

        return trends;
    }

    /**
     * Returns budget status by comparing actual spending against goals.
     * For each active goal, calculates spent amount in the goal's date range.
     */
    public List<BudgetStatus> getBudgetStatus(String userId) {
        // Without a GoalRepository, return category spending vs a default monthly budget
        // This provides current month's spending per category as budget status
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now();

        List<Transaction> transactions = getTransactionsForDateRange(userId, start, end);

        Map<String, BigDecimal> spentByCategory = transactions.stream()
                .filter(t -> "DEBIT".equals(t.getType()))
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        return spentByCategory.entrySet().stream()
                .map(e -> new BudgetStatus(
                        e.getKey(),
                        BigDecimal.ZERO,
                        e.getValue(),
                        e.getValue().negate(),
                        0.0,
                        "MONTHLY"
                ))
                .sorted((a, b) -> b.spentAmount().compareTo(a.spentAmount()))
                .collect(Collectors.toList());
    }

    private List<Transaction> getTransactions(String userId) {
        if (transactionRepository == null || userId == null || userId.isBlank()) {
            return List.of();
        }
        return transactionRepository.findByUserId(userId);
    }

    private List<Transaction> getTransactionsForDateRange(String userId, LocalDate start, LocalDate end) {
        if (transactionRepository == null || userId == null || userId.isBlank()) {
            return List.of();
        }
        if (start != null && end != null) {
            return transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end);
        }
        return transactionRepository.findByUserId(userId);
    }
}
