package com.georgesbouanni.controle_gastos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome deve ser obrigatório")
    private String name;

    @Column(unique = true)
    @NotBlank(message = "O CPF deve ser obrigatório")
    private String cpf;

    @Column(unique = true)
    @NotBlank(message = "O telefone deve ser obrigatório")
    private String numeroTelefone;

    @NotNull(message = "A data de nascimento deve ser obrigatória")
    private LocalDate dataNascimento;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "A senha deve ser obrigatória")
    private String senhaHash;

    @Column(unique = true)
    @NotBlank(message = "O email deve ser obrigatório")
    private String email;

    @NotNull(message = "O valor deve ser obrigatório")
    @PositiveOrZero(message = "O saldo não pode ser negativo")
    private BigDecimal balance;

    @JsonIgnore
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int failedLoginAttempts = 0;

    @JsonIgnore
    private LocalDateTime lockedUntil;

    @JsonIgnore
    @Column(name = "account_closed", nullable = false, columnDefinition = "boolean default false")
    private boolean accountClosed = false;

    @JsonIgnore
    @Column(name = "credential_version", nullable = false, columnDefinition = "integer default 0")
    private int credentialVersion = 0;

    public boolean isAccountClosed() { return accountClosed; }
    public void setAccountClosed(boolean accountClosed) { this.accountClosed = accountClosed; }
    public int getCredentialVersion() { return credentialVersion; }
    public void setCredentialVersion(int credentialVersion) { this.credentialVersion = credentialVersion; }

    @JsonIgnore
    @Version
    @Column(name = "row_version", nullable = false, columnDefinition = "bigint default 0")
    private Long rowVersion = 0L;

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

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

}