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
     * <p>
     * Uses database-level aggregate queries (SUM, COUNT, GROUP BY) instead of
     * loading all transactions into memory. This is dramatically faster for
     * users with many transactions.
     */
    public SpendingSummary getSpendingSummary(String userId) {
        if (transactionRepository == null || userId == null || userId.isBlank()) {
            return new SpendingSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "N/A", 0);
        }

        BigDecimal totalIncome = transactionRepository.sumIncomeByUserId(userId);
        BigDecimal totalExpense = transactionRepository.sumExpenseByUserId(userId);
        BigDecimal netSavings = totalIncome.subtract(totalExpense);

        // Find top spending category via DB aggregation
        String topCategory = "N/A";
        List<Object[]> topCatResult = transactionRepository.findTopCategoryByUserId(userId);
        if (!topCatResult.isEmpty() && topCatResult.get(0)[0] != null) {
            topCategory = (String) topCatResult.get(0)[0];
        }

        int txnCount = transactionRepository.countByUserId(userId);

        return new SpendingSummary(totalIncome, totalExpense, netSavings, topCategory, txnCount);
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
        return getMonthlyTrends(userId, months, "MONTHLY");
    }

    /**
     * Returns income/expense/net trends grouped by the specified period.
     * Supports MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY.
     */
    public List<MonthlyTrend> getMonthlyTrends(String userId, int periods, String period) {
        if (periods <= 0) {
            periods = getDefaultPeriods(period);
        }
        String effectivePeriod = (period == null || period.isBlank()) ? "MONTHLY" : period.toUpperCase();

        List<Transaction> transactions = getTransactions(userId);

        // Group transactions by period key
        Map<String, List<Transaction>> byPeriod = transactions.stream()
                .filter(t -> t.getTransactionDate() != null)
                .collect(Collectors.groupingBy(t -> getPeriodKey(t.getTransactionDate(), effectivePeriod)));

        // Generate the last N period labels in order
        List<String> periodLabels = generatePeriodLabels(periods, effectivePeriod);

        List<MonthlyTrend> trends = new ArrayList<>();
        for (String label : periodLabels) {
            List<Transaction> periodTxns = byPeriod.getOrDefault(label, List.of());
            BigDecimal income = periodTxns.stream()
                    .filter(t -> "CREDIT".equals(t.getType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal expense = periodTxns.stream()
                    .filter(t -> "DEBIT".equals(t.getType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            trends.add(new MonthlyTrend(label, income, expense, income.subtract(expense)));
        }

        return trends;
    }

    /**
     * Compares current period spending against the previous period.
     * Returns overall totals plus per-category breakdown of changes.
     */
    public SpendingComparison getSpendingComparison(String userId, String period) {
        String effectivePeriod = (period == null || period.isBlank()) ? "MONTHLY" : period.toUpperCase();

        // Determine current and previous period date ranges
        LocalDate[] currentRange = getCurrentPeriodRange(effectivePeriod);
        LocalDate[] previousRange = getPreviousPeriodRange(effectivePeriod);

        String currentLabel = getPeriodLabel(currentRange[0], effectivePeriod);
        String previousLabel = getPeriodLabel(previousRange[0], effectivePeriod);

        List<Transaction> currentTxns = getTransactionsForDateRange(userId, currentRange[0], currentRange[1]);
        List<Transaction> previousTxns = getTransactionsForDateRange(userId, previousRange[0], previousRange[1]);

        BigDecimal currentTotal = currentTxns.stream()
                .filter(t -> "DEBIT".equals(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal previousTotal = previousTxns.stream()
                .filter(t -> "DEBIT".equals(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal changeAmount = currentTotal.subtract(previousTotal);
        double changePercentage = previousTotal.compareTo(BigDecimal.ZERO) > 0
                ? changeAmount.multiply(BigDecimal.valueOf(100))
                        .divide(previousTotal, 2, RoundingMode.HALF_UP)
                        .doubleValue()
                : (currentTotal.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0);

        // Per-category comparison
        Map<String, BigDecimal> currentByCategory = currentTxns.stream()
                .filter(t -> "DEBIT".equals(t.getType()))
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        Map<String, BigDecimal> previousByCategory = previousTxns.stream()
                .filter(t -> "DEBIT".equals(t.getType()))
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        Set<String> allCategories = new HashSet<>();
        allCategories.addAll(currentByCategory.keySet());
        allCategories.addAll(previousByCategory.keySet());

        List<CategoryChange> categoryChanges = allCategories.stream()
                .map(cat -> {
                    BigDecimal curr = currentByCategory.getOrDefault(cat, BigDecimal.ZERO);
                    BigDecimal prev = previousByCategory.getOrDefault(cat, BigDecimal.ZERO);
                    BigDecimal catChange = curr.subtract(prev);
                    double catPct = prev.compareTo(BigDecimal.ZERO) > 0
                            ? catChange.multiply(BigDecimal.valueOf(100))
                                    .divide(prev, 2, RoundingMode.HALF_UP)
                                    .doubleValue()
                            : (curr.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0);
                    return new CategoryChange(cat, curr, prev, catChange, catPct);
                })
                .sorted((a, b) -> b.changeAmount().abs().compareTo(a.changeAmount().abs()))
                .collect(Collectors.toList());

        return new SpendingComparison(
                currentLabel, previousLabel,
                currentTotal, previousTotal,
                changeAmount, changePercentage,
                categoryChanges
        );
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

    // ---- Period helpers ----

    private int getDefaultPeriods(String period) {
        return switch (period) {
            case "QUARTERLY" -> 4;
            case "HALF_YEARLY" -> 4;
            case "YEARLY" -> 3;
            default -> 6;
        };
    }

    private String getPeriodKey(LocalDate date, String period) {
        return switch (period) {
            case "QUARTERLY" -> {
                int quarter = (date.getMonthValue() - 1) / 3 + 1;
                yield date.getYear() + "-Q" + quarter;
            }
            case "HALF_YEARLY" -> {
                int half = date.getMonthValue() <= 6 ? 1 : 2;
                yield date.getYear() + "-H" + half;
            }
            case "YEARLY" -> String.valueOf(date.getYear());
            default -> date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        };
    }

    private String getPeriodLabel(LocalDate date, String period) {
        return getPeriodKey(date, period);
    }

    private List<String> generatePeriodLabels(int periods, String period) {
        List<String> labels = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = periods - 1; i >= 0; i--) {
            LocalDate anchor = switch (period) {
                case "QUARTERLY" -> now.minusMonths((long) i * 3);
                case "HALF_YEARLY" -> now.minusMonths((long) i * 6);
                case "YEARLY" -> now.minusYears(i);
                default -> now.minusMonths(i);
            };
            labels.add(getPeriodKey(anchor, period));
        }
        return labels;
    }

    private LocalDate[] getCurrentPeriodRange(String period) {
        LocalDate now = LocalDate.now();
        return switch (period) {
            case "QUARTERLY" -> {
                int quarter = (now.getMonthValue() - 1) / 3 + 1;
                int startMonth = (quarter - 1) * 3 + 1;
                yield new LocalDate[]{
                        LocalDate.of(now.getYear(), startMonth, 1),
                        LocalDate.of(now.getYear(), startMonth, 1).plusMonths(3).minusDays(1)
                };
            }
            case "HALF_YEARLY" -> {
                if (now.getMonthValue() <= 6) {
                    yield new LocalDate[]{LocalDate.of(now.getYear(), 1, 1), LocalDate.of(now.getYear(), 6, 30)};
                } else {
                    yield new LocalDate[]{LocalDate.of(now.getYear(), 7, 1), LocalDate.of(now.getYear(), 12, 31)};
                }
            }
            case "YEARLY" -> new LocalDate[]{
                    LocalDate.of(now.getYear(), 1, 1),
                    LocalDate.of(now.getYear(), 12, 31)
            };
            default -> new LocalDate[]{
                    now.withDayOfMonth(1),
                    now.withDayOfMonth(1).plusMonths(1).minusDays(1)
            };
        };
    }

    private LocalDate[] getPreviousPeriodRange(String period) {
        LocalDate now = LocalDate.now();
        return switch (period) {
            case "QUARTERLY" -> {
                int quarter = (now.getMonthValue() - 1) / 3 + 1;
                int startMonth = (quarter - 1) * 3 + 1;
                LocalDate currStart = LocalDate.of(now.getYear(), startMonth, 1);
                LocalDate prevEnd = currStart.minusDays(1);
                LocalDate prevStart = prevEnd.withDayOfMonth(1).minusMonths(2);
                yield new LocalDate[]{prevStart, prevEnd};
            }
            case "HALF_YEARLY" -> {
                if (now.getMonthValue() <= 6) {
                    yield new LocalDate[]{LocalDate.of(now.getYear() - 1, 7, 1), LocalDate.of(now.getYear() - 1, 12, 31)};
                } else {
                    yield new LocalDate[]{LocalDate.of(now.getYear(), 1, 1), LocalDate.of(now.getYear(), 6, 30)};
                }
            }
            case "YEARLY" -> new LocalDate[]{
                    LocalDate.of(now.getYear() - 1, 1, 1),
                    LocalDate.of(now.getYear() - 1, 12, 31)
            };
            default -> {
                LocalDate prevEnd = now.withDayOfMonth(1).minusDays(1);
                LocalDate prevStart = prevEnd.withDayOfMonth(1);
                yield new LocalDate[]{prevStart, prevEnd};
            }
        };
    }
}
