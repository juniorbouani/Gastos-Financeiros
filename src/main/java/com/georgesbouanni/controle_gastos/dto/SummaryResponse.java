package com.georgesbouanni.controle_gastos.dto;

import java.math.BigDecimal;

public class SummaryResponse {

    private String month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;

    public SummaryResponse(String month, BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal balance) {
        this.month = month;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.balance = balance;
    }

    public String getMonth() {
        return month;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
