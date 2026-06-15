package com.tracker.MoneyTracker.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    // Default constructor for framework / test support if needed
    public TransactionController() {
        this.transactionService = null;
    }

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId) {

        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("userId is required");
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            return ResponseEntity.badRequest().body("Filename is null");
        }

        String lowerName = fileName.toLowerCase();
        if (!lowerName.endsWith(".csv") && !lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")) {
            return ResponseEntity.badRequest().body("Unsupported file type");
        }

        try {
            Path tempPath = Files.createTempFile("upload-", fileName);
            File tempFile = tempPath.toFile();
            file.transferTo(tempFile);

            try {
                if (transactionService != null) {
                    transactionService.processStatement(tempFile, userId);
                }
                return ResponseEntity.status(HttpStatus.CREATED).build();
            } finally {
                tempFile.delete();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process file: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(@RequestParam("userId") String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (transactionService == null) {
            return ResponseEntity.ok(List.of());
        }
        List<Transaction> transactions = transactionService.getTransactionsByUser(userId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, BigDecimal>> getCategorySummary(@RequestParam("userId") String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (transactionService == null) {
            return ResponseEntity.ok(Map.of());
        }
        Map<String, BigDecimal> summary = transactionService.getCategorySummary(userId);
        return ResponseEntity.ok(summary);
    }
}

