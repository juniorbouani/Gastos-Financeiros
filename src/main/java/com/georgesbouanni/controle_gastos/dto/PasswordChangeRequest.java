package com.georgesbouanni.controle_gastos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados necessários para validar a senha atual e definir uma nova senha. */
public record PasswordChangeRequest(
        @NotBlank(message = "Informe a senha atual.") String senhaAtual,
        @NotBlank(message = "Informe a nova senha.")
                @Size(min = 8, max = 72, message = "A nova senha deve ter entre 8 e 72 caracteres.")
                String novaSenha) {}
