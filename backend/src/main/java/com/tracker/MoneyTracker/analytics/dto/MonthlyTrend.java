package com.tracker.MoneyTracker.analytics.dto;

import java.math.BigDecimal;

/**
 * Monthly income/expense/net trend data.
 *
 * @param month  the month in "YYYY-MM" format
 * @param income total credit amount for the month
 * @param expense total debit amount for the month
 * @param net    net savings (income - expense)
 */
public record MonthlyTrend(String month, BigDecimal income, BigDecimal expense, BigDecimal net) {
}
