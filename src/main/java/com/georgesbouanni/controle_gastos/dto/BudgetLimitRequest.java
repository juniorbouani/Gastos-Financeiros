package com.georgesbouanni.controle_gastos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class BudgetLimitRequest {

    @NotNull(message = "O novo limite é obrigatório!")
    @Positive(message = "O limite deve ser positivo!")
    private BigDecimal limite;

    public BigDecimal getLimite() {
        return limite;
    }

    public void setLimite(BigDecimal limite) {
        this.limite = limite;
    }
}
