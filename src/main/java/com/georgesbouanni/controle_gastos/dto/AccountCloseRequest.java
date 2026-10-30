package com.georgesbouanni.controle_gastos.dto;
import jakarta.validation.constraints.NotBlank;
public record AccountCloseRequest(
        @NotBlank(message = "Informe a senha atual.") String senhaAtual,
        @NotBlank(message = "Confirme o encerramento.") String confirmacao) {}
