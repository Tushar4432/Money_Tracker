package com.tracker.MoneyTracker.transaction;

import com.tracker.MoneyTracker.error.ErrorCode;
import com.tracker.MoneyTracker.exception.BadRequestException;
import com.tracker.MoneyTracker.goal.SpendingGoal;
import com.tracker.MoneyTracker.goal.SpendingGoalRepository;
import com.tracker.MoneyTracker.notification.Notification;
import com.tracker.MoneyTracker.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final FileParser fileParser;
    private final CategoryClassifier categoryClassifier;
    private final SpendingGoalRepository spendingGoalRepository;
    private final NotificationService notificationService;

    public TransactionService() {
        this.transactionRepository = null;
        this.fileParser = new FileParser();
        this.categoryClassifier = new CategoryClassifier(new CategoryMapper());
        this.spendingGoalRepository = null;
        this.notificationService = null;
    }

    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                              FileParser fileParser,
                              CategoryClassifier categoryClassifier) {
        this.transactionRepository = transactionRepository;
        this.fileParser = fileParser;
        this.categoryClassifier = categoryClassifier;
        this.spendingGoalRepository = null;
        this.notificationService = null;
    }

    public TransactionService(TransactionRepository transactionRepository,
                              FileParser fileParser,
                              CategoryClassifier categoryClassifier,
                              SpendingGoalRepository spendingGoalRepository,
                              NotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.fileParser = fileParser;
        this.categoryClassifier = categoryClassifier;
        this.spendingGoalRepository = spendingGoalRepository;
        this.notificationService = notificationService;
    }

    public List<Transaction> processStatement(File file, String userId) throws Exception {
        if (file == null || userId == null || userId.trim().isEmpty()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "File and userId must not be null/blank");
        }

        List<Map<String, String>> parsedRows;
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".csv")) {
            parsedRows = fileParser.parseCsv(file);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            parsedRows = fileParser.parseExcel(file);
        } else {
            throw new BadRequestException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        List<Transaction> transactions = new ArrayList<>();
        for (Map<String, String> row : parsedRows) {
            String dateStr = row.get("date");
            LocalDate date = parseDate(dateStr);
            if (date == null) {
                continue; // skip rows with unparseable dates
            }

            String debitVal = row.get("debit");
            String creditVal = row.get("credit");
            BigDecimal amount = BigDecimal.ZERO;
            String type = "";

            if (debitVal != null && !debitVal.trim().isEmpty()) {
                try {
                    amount = new BigDecimal(debitVal.trim().replace(",", ""));
                    type = "DEBIT";
                } catch (Exception e) {
                    amount = BigDecimal.ONE;
                    type = "DEBIT";
                }
            } else if (creditVal != null && !creditVal.trim().isEmpty()) {
                try {
                    amount = new BigDecimal(creditVal.trim().replace(",", ""));
                    type = "CREDIT";
                } catch (Exception e) {
                    amount = BigDecimal.ONE;
                    type = "CREDIT";
                }
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                amount = BigDecimal.ONE;
            }
            if (type == null || type.isEmpty()) {
                type = "DEBIT";
            }

            String details = row.get("details");
            String description = cleanDescription(details);
            if (description == null || description.isBlank()) {
                description = "Unknown";
            }

            Transaction tx = new Transaction();
            tx.setId(UUID.randomUUID().toString());
            tx.setUserId(userId);
            tx.setTransactionDate(date);
            tx.setAmount(amount.abs());
            tx.setType(type);
            tx.setDescription(description);
            tx.setOriginalDetail(details);
            tx.setCategory(categoryClassifier.classify(details));
            tx.setSentTo(extractSentTo(details));
            tx.setCreatedAt(LocalDateTime.now());
            tx.setUpdatedAt(LocalDateTime.now());
            // transactionHash is auto-computed in @PrePersist

            transactions.add(tx);
        }

        if (transactionRepository != null && !transactions.isEmpty()) {
            try {
                transactionRepository.saveAll(transactions);
            } catch (DataIntegrityViolationException e) {
                // Unique constraint (uq_transaction_hash) violated — duplicate upload.
                // Save one by one so new transactions still get persisted.
                log.warn("Bulk save failed for user {}, falling back to individual saves", userId);
                int saved = 0, skipped = 0;
                for (Transaction tx : transactions) {
                    try {
                        transactionRepository.save(tx);
                        saved++;
                    } catch (DataIntegrityViolationException dup) {
                        skipped++;
                    }
                }
                log.info("Saved {} new transactions, skipped {} duplicates for user {}", saved, skipped, userId);
            }
        }

        evaluateAndNotify(userId);

        return transactions;
    }

    public List<Transaction> getTransactionsByUser(String userId) {
        if (transactionRepository == null) {
            return List.of();
        }
        return transactionRepository.findByUserId(userId);
    }

    public List<Transaction> getTransactionsByUserAndDateRange(String userId, LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (transactionRepository == null) {
            return List.of();
        }
        return transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end);
    }

    public Map<String, BigDecimal> getCategorySummary(String userId) {
        if (transactionRepository == null) {
            return Map.of();
        }
        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        return transactions.stream()
                .filter(t -> t.getCategory() != null && t.getAmount() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Transaction::getAmount,
                                BigDecimal::add
                        )
                ));
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String cleanDate = dateStr.trim();
        List<String> patterns = List.of("dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy");
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(cleanDate, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String cleanDescription(String details) {
        if (details == null) {
            return "";
        }
        String clean = details.replaceAll("/\\d+/", "/");
        if (clean.toUpperCase().contains("UPI/")) {
            int upiIdx = clean.toUpperCase().indexOf("UPI/");
            if (upiIdx != -1) {
                clean = clean.substring(upiIdx);
            }
        }
        return clean;
    }

    /**
     * Evaluates all active goals for the user and creates notifications
     * when spending is approaching or exceeding the target.
     * Called automatically after statement upload.
     */
    public void evaluateAndNotify(String userId) {
        if (spendingGoalRepository == null || notificationService == null
                || userId == null || userId.isBlank()) {
            return;
        }

        List<SpendingGoal> activeGoals = spendingGoalRepository.findByUserIdAndActive(userId, true);
        if (activeGoals.isEmpty()) {
            return;
        }

        for (SpendingGoal goal : activeGoals) {
            LocalDate start = goal.getStartDate() != null ? goal.getStartDate() : LocalDate.now().withDayOfMonth(1);
            LocalDate end = goal.getEndDate() != null ? goal.getEndDate() : LocalDate.now();

            List<Transaction> transactions = transactionRepository != null
                    ? transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end)
                    : List.of();

            BigDecimal spent = transactions.stream()
                    .filter(t -> "DEBIT".equals(t.getType()))
                    .filter(t -> goal.getCategory().equals(t.getCategory()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal target = goal.getTargetAmount();
            if (target == null || target.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            double percentage = spent.multiply(BigDecimal.valueOf(100))
                    .divide(target, 2, RoundingMode.HALF_UP)
                    .doubleValue();

            if (percentage >= 100.0) {
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setType("GOAL_EXCEEDED");
                notification.setTitle(goal.getCategory());
                notification.setMessage("You have exceeded your " + goal.getCategory()
                        + " budget of " + target + ". Spent: " + spent);
                notification.setReferenceId(goal.getId());
                notificationService.createNotification(notification);
            } else if (percentage >= 80.0) {
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setType("GOAL_WARNING");
                notification.setTitle(goal.getCategory());
                notification.setMessage("You have used " + percentage + "% of your "
                        + goal.getCategory() + " budget. Spent: " + spent + " / " + target);
                notification.setReferenceId(goal.getId());
                notificationService.createNotification(notification);
            }
        }
    }

    private String extractSentTo(String details) {
        if (details == null || details.trim().isEmpty()) {
            return null;
        }
        String clean = details.trim();
        if (clean.toUpperCase().contains("UPI/")) {
            String[] parts = clean.split("/");
            int drIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equalsIgnoreCase("DR") || parts[i].equalsIgnoreCase("CR")) {
                    drIndex = i;
                    break;
                }
            }
            if (drIndex != -1 && drIndex + 1 < parts.length) {
                String next = parts[drIndex + 1];
                if (next.matches("\\d+") && drIndex + 2 < parts.length) {
                    return parts[drIndex + 2].trim();
                } else {
                    return next.trim();
                }
            }
        }
        return null;
    }
}

