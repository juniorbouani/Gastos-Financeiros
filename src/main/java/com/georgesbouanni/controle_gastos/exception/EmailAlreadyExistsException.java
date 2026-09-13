package com.georgesbouanni.controle_gastos.exception;

public class EmailAlreadyExistsException  extends RuntimeException{

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
