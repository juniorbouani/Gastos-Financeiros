package com.georgesbouanni.controle_gastos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Year;
import java.time.YearMonth;

public class BudgetRequest {

    @NotNull(message = "O usuário é obrigatório")
    private Long userId;

    @NotNull(message = "A categoria é obrigatória!")
    private Long categoryId;

    @NotNull(message = "O mês referência é obrigatório!")
    private YearMonth mesReferencia;

    @NotNull(message = "O limite é obrigatório!")
    @Positive(message = "O limite deve ser positivo!")
    private BigDecimal limite;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public YearMonth getMesReferencia() {
        return mesReferencia;
    }

    public void setMesReferencia(YearMonth mesReferencia) {
        this.mesReferencia = mesReferencia;
    }

    public BigDecimal getLimite() {
        return limite;
    }

    public void setLimite(BigDecimal limite) {
        this.limite = limite;
    }
}