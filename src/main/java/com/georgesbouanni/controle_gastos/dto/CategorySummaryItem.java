package com.georgesbouanni.controle_gastos.dto;

import java.math.BigDecimal;

public class CategorySummaryItem {

    private final Long categoryId;
    private final String categoryName;
    private final BigDecimal total;
    private final long transactions;
    private final BigDecimal percentage;
    private final BigDecimal budgetLimit;
    private final BigDecimal budgetRemaining;

    public CategorySummaryItem(
            Long categoryId,
            String categoryName,
            BigDecimal total,
            long transactions,
            BigDecimal percentage,
            BigDecimal budgetLimit,
            BigDecimal budgetRemaining) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.total = total;
        this.transactions = transactions;
        this.percentage = percentage;
        this.budgetLimit = budgetLimit;
        this.budgetRemaining = budgetRemaining;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public long getTransactions() {
        return transactions;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public BigDecimal getBudgetLimit() {
        return budgetLimit;
    }

    public BigDecimal getBudgetRemaining() {
        return budgetRemaining;
    }
}
