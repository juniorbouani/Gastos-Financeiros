package com.georgesbouanni.controle_gastos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class CreditCardRequest {

    @NotNull(message = "O usuário é obrigatório")
    private Long userId;

    @NotNull(message = "O limite total é obrigatório")
    @Positive(message = "O limite deve ser positivo")
    private BigDecimal limiteTotal;

    private boolean descartavel;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getLimiteTotal() {
        return limiteTotal;
    }

    public void setLimiteTotal(BigDecimal limiteTotal) {
        this.limiteTotal = limiteTotal;
    }

    public boolean isDescartavel() {
        return descartavel;
    }

    public void setDescartavel(boolean descartavel) {
        this.descartavel = descartavel;
    }
}
