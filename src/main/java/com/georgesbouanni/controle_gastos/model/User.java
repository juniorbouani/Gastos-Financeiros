package com.georgesbouanni.controle_gastos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome deve ser obrigatório")
    private String name;

    @NotBlank(message = "O CPF deve ser obrigatório")
    private String cpf;

    @NotBlank(message = "O telefone deve ser obrigatório")
    private String numeroTelefone;

    @NotNull(message = "A data de nascimento deve ser obrigatória")
    private LocalDate dataNascimento;

    @NotBlank(message = "A senha deve ser obrigatória")
    private String senhaHash;

    @NotBlank(message = "O email deve ser obrigatório")
    private String email;

    @NotNull(message = "O valor deve ser obrigatório")
    @PositiveOrZero(message = "O saldo não pode ser negativo")
    private BigDecimal balance;

    public User() {
    }

    public User(String name, String cpf, String numeroTelefone, LocalDate dataNascimento, String senhaHash, String email, BigDecimal balance) {
        this.name = name;
        this.cpf = cpf;
        this.numeroTelefone = numeroTelefone;
        this.dataNascimento = dataNascimento;
        this.senhaHash = senhaHash;
        this.email = email;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getNumeroTelefone() {
        return numeroTelefone;
    }

    public void setNumeroTelefone(String numeroTelefone) {
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