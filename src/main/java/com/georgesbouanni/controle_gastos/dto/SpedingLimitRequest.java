package com.georgesbouanni.controle_gastos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class SpedingLimitRequest {

    @NotNull(message = "O novo limite é obrigatório")
    @PositiveOrZero(message = "O limitenão pode ser negativo")
    private BigDecimal novoLimite;

    public BigDecimal getNovoLimite() {
        return novoLimite;
    }

    public void setNovoLimite(BigDecimal novoLimite) {
        this.novoLimite = novoLimite;
    }
}
