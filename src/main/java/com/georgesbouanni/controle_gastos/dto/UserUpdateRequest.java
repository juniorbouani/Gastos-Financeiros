package com.georgesbouanni.controle_gastos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class UserUpdateRequest {

    @NotBlank(message = "O nome é obrigatório!")
    private String name;

    @NotBlank(message = "O telefone é obrigatório!")
    private String numeroTelefone;

    @NotBlank(message = "O e-mail é obrigatório!")
    @Email(message = "E-mail inválido!")
    private String email;

    @NotNull(message = "A data de nascimento é obrigatória!")
    private LocalDate dataNascimento;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumeroTelefone() {
        return numeroTelefone;
    }

    public void setNumeroTelefone(String numeroTelefone) {
        this.numeroTelefone = numeroTelefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }
}
