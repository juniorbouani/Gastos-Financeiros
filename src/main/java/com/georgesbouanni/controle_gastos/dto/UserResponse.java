package com.georgesbouanni.controle_gastos.dto;

import com.georgesbouanni.controle_gastos.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UserResponse {

    private final Long id;
    private final String name;
    private final String cpf;
    private final String numeorTelefone;
    private final LocalDate dataNascimento;
    private final String email;
    private final BigDecimal balance;

    public UserResponse(User u) {
        this.id = u.getId();
        this.name = u.getName();
        this.cpf = u.getCpf();
        this.numeorTelefone = u.getNumeroTelefone();
        this.dataNascimento = u.getDataNascimento();
        this.email = u.getEmail();
        this.balance = u.getBalance();
    }

    public static UserResponse from(User user) {
        return new UserResponse(user);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCpf() {
        return cpf;
    }

    public String getNumeorTelefone() {
        return numeorTelefone;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getEmail() {
        return email;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
