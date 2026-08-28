package com.georgesbouanni.controle_gastos.model;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "/Users")
public class User {

    @Id
    private String id;

    @NotBlank(message = "O nome deve ser obrigatório")
    private String name;

    @NotNull(message = "O CPF deve ser obrigatório")
    private BigDecimal cpf;

    @NotNull(message = "O telefone deve4 ser obrigatório")
    private BigDecimal numeroTelefone;

    @NotNull(message = "A data de nascimento deve ser obrigatória")
    private LocalDate dataNascimento;

    @NotBlank(message = "A senha deve ser obrigatória")
    private String senhaHash;

    @NotBlank(message = "O email deve ser obrigatório")
    private String email;

    @NotNull(message = "O valor deve ser obrigatório")
    @Positive(message = "o valor deve ser postivo")
    private BigDecimal balance;

    public User(String id, String name, BigDecimal cpf, BigDecimal numeroTelefone, LocalDate dataNascimento, String senhaHash, String email, BigDecimal balance) {
        this.id = id;
        this.name = name;
        this.cpf = cpf;
        this.numeroTelefone = numeroTelefone;
        this.dataNascimento = dataNascimento;
        this.senhaHash = senhaHash;
        this.email = email;
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getCpf() {
        return cpf;
    }

    public void setCpf(BigDecimal cpf) {
        this.cpf = cpf;
    }

    public BigDecimal getNumeroTelefone() {
        return numeroTelefone;
    }

    public void setNumeroTelefone(BigDecimal numeroTelefone) {
        this.numeroTelefone = numeroTelefone;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}