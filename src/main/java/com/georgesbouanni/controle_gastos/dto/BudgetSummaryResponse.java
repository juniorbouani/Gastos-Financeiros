package com.georgesbouanni.controle_gastos.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public class BudgetSummaryResponse {

    private final Long budgetId;
    private final String categoria;
    private final YearMonth mesReferencia;
    private final BigDecimal limite;
    private final BigDecimal gasto;

    public BudgetSummaryResponse(
            Long budgetId,
            String categoria,
            YearMonth mesReferencia,
            BigDecimal limite,
            BigDecimal gasto) {
        this.budgetId = budgetId;
        this.categoria = categoria;
        this.mesReferencia = mesReferencia;
        this.limite = limite;
        this.gasto = gasto;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public String getCategoria() {
        return categoria;
    }

    public YearMonth getMesReferencia() {
        return mesReferencia;
    }

    public BigDecimal getLimite() {
        return limite;
    }

    public BigDecimal getGasto() {
        return gasto;
    }

    public BigDecimal getRestante() {
        return limite.subtract(gasto);
    }

    public boolean isEstourado() {
        return gasto.compareTo(limite) > 0;
    }
}
