package com.georgesbouanni.controle_gastos.exception;

public class InsufficientCreditLimitException extends RuntimeException {
    public InsufficientCreditLimitException(String message) {
        super(message);
    }
}
