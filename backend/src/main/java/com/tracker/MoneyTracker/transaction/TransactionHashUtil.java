package com.tracker.MoneyTracker.transaction;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

/**
 * Utility for computing a deterministic SHA-256 hash from transaction fields.
 * Used to detect duplicate transactions when re-uploading bank statements.
 *
 * The hash is computed from: userId + transactionDate + amount + type + originalDetail
 * These fields together form a natural key — two transactions with identical values
 * across all five are considered the same transaction.
 */
public final class TransactionHashUtil {

    private TransactionHashUtil() {
        // utility class
    }

    /**
     * Computes a SHA-256 hex string from the transaction's natural key fields.
     *
     * @param userId          the user ID
     * @param date            the transaction date
     * @param amount          the absolute amount
     * @param type            DEBIT or CREDIT
     * @param originalDetail  the raw bank statement detail text
     * @return 64-character lowercase hex SHA-256 hash
     */
    public static String compute(String userId, LocalDate date, BigDecimal amount, String type, String originalDetail) {
        String raw = (userId != null ? userId : "") + "|"
                + (date != null ? date.toString() : "") + "|"
                + (amount != null ? amount.toPlainString() : "") + "|"
                + (type != null ? type : "") + "|"
                + (originalDetail != null ? originalDetail : "");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to exist on every JVM — this should never happen
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
