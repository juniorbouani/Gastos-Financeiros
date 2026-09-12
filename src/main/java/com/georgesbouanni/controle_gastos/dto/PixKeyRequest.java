package com.georgesbouanni.controle_gastos.dto;

import com.georgesbouanni.controle_gastos.model.PixKeyType;
import jakarta.validation.constraints.NotNull;

public class PixKeyRequest {

    @NotNull(message = "O usuáiro é obrigatório!")
    private Long userId;

    @NotNull(message = "O tipo da chave é obrigatório!")
    private PixKeyType keyType;

    private String keyValue;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public PixKeyType getKeyType() {
        return keyType;
    }

    public void setKeyType(PixKeyType keyType) {
        this.keyType = keyType;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }
}
