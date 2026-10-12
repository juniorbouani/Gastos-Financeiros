package com.georgesbouanni.controle_gastos.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public class BudgetSummaryResponse {

    private Long budgetId;
    private String categoria;
    private YearMonth mesReferencia;
    private BigDecimal limite;
    private BigDecimal gasto;
    private BigDecimal restante;
    private boolean estourado;

    public BudgetSummaryResponse(Long budgetId, String categoria, YearMonth mesReferencia,
                                 BigDecimal limite, BigDecimal gasto) {
        this.categoria = categoria;
        this.budgetId = budgetId;
        this.mesReferencia = mesReferencia;
        this.limite = limite;
        this.gasto = gasto;
        this.restante = restante;
        this.estourado = estourado;
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
        return restante;
    }

    public boolean isEstourado() {
        return estourado;
    }
}
