package com.tracker.MoneyTracker.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final FileParser fileParser;
    private final CategoryClassifier categoryClassifier;

    public TransactionService() {
        this.transactionRepository = null;
        this.fileParser = new FileParser();
        this.categoryClassifier = new CategoryClassifier();
    }

    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                              FileParser fileParser,
                              CategoryClassifier categoryClassifier) {
        this.transactionRepository = transactionRepository;
        this.fileParser = fileParser;
        this.categoryClassifier = categoryClassifier;
    }

    public List<Transaction> processStatement(File file, String userId) throws Exception {
        if (file == null || userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("File and userId must not be null/blank");
        }

        List<Map<String, String>> parsedRows;
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".csv")) {
            parsedRows = fileParser.parseCsv(file);
        } else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            parsedRows = fileParser.parseExcel(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type");
        }

        List<Transaction> transactions = new ArrayList<>();
        for (Map<String, String> row : parsedRows) {
            Transaction tx = new Transaction();
            tx.setId(UUID.randomUUID().toString());
            tx.setUserId(userId);

            String dateStr = row.get("date");
            tx.setTransactionDate(parseDate(dateStr));

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

            tx.setAmount(amount.abs());
            tx.setType(type);

            String details = row.get("details");
            tx.setDescription(cleanDescription(details));
            tx.setOriginalDetail(details);
            tx.setCategory(categoryClassifier.classify(details));
            tx.setSentTo(extractSentTo(details));

            tx.setCreatedAt(LocalDateTime.now());
            tx.setUpdatedAt(LocalDateTime.now());

            transactions.add(tx);
        }

        if (transactionRepository != null) {
            transactionRepository.saveAll(transactions);
        }

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
            throw new IllegalArgumentException("Start date cannot be after end date");
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

