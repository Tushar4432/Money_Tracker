package com.tracker.MoneyTracker.transaction;

import com.tracker.MoneyTracker.error.ErrorCode;
import com.tracker.MoneyTracker.exception.BadRequestException;
import com.tracker.MoneyTracker.exception.InternalServerException;
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

    public TransactionController() {
        this.transactionService = null;
    }

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Void> uploadStatement(
            @RequestParam("bank_statements") MultipartFile file,
            @RequestParam(value = "userId", required = false) String userId) {

        if (userId == null || userId.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "userId is required");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "File is empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "Filename is null");
        }

        String lowerName = fileName.toLowerCase();
        if (!lowerName.endsWith(".csv") && !lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")) {
            throw new BadRequestException(ErrorCode.UNSUPPORTED_FILE_TYPE);
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
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactions(@RequestParam("userId") String userId) {
        if (transactionService == null) {
            return ResponseEntity.ok(List.of());
        }
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "userId is required");
        }
        return ResponseEntity.ok(transactionService.getTransactionsByUser(userId));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, BigDecimal>> getCategorySummary(@RequestParam("userId") String userId) {
        if (transactionService == null) {
            return ResponseEntity.ok(Map.of());
        }
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException(ErrorCode.INVALID_INPUT, "userId is required");
        }
        return ResponseEntity.ok(transactionService.getCategorySummary(userId));
    }
}
