package com.georgesbouanni.controle_gastos.dto;

import com.georgesbouanni.controle_gastos.model.PixKeyType;

public class PixKeyLookupResponse {

    private final PixKeyType keyType;
    private final String keyValue;
    private final String titular;

    public PixKeyLookupResponse(PixKeyType keyType, String keyValue, String titular) {
        this.keyType = keyType;
        this.keyValue = keyValue;
        this.titular = titular;
    }

    public PixKeyType getKeyType() {
        return keyType;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public String getTitular() {
        return titular;
    }
}
