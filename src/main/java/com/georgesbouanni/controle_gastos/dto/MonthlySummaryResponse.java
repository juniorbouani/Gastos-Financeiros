package com.georgesbouanni.controle_gastos.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class MonthlySummaryResponse {

    private final YearMonth month;
    private final BigDecimal totalSpent;
    private final BigDecimal totalReceived;
    private final List<CategorySummaryItem> categories;

    public MonthlySummaryResponse(
            YearMonth month,
            BigDecimal totalSpent,
            BigDecimal totalReceived,
            List<CategorySummaryItem> categories) {
        this.month = month;
        this.totalSpent = totalSpent;
        this.totalReceived = totalReceived;
        this.categories = categories;
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public BigDecimal getTotalReceived() {
        return totalReceived;
    }

    public List<CategorySummaryItem> getCategories() {
        return categories;
    }

    public BigDecimal getNet() {
        return totalReceived.subtract(totalSpent);
    }
}
