package com.georgesbouanni.controle_gastos.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record ShopCheckoutRequest(
 @NotBlank(message="Selecione o produto.") String productId,
 @NotNull(message="Selecione o cartão.") Long cardId,
 @Min(value=1,message="Informe de 1 a 12 parcelas.") @Max(value=12,message="Informe de 1 a 12 parcelas.") int totalParcelas,
 @NotNull(message="O identificador do pedido é obrigatório.") UUID requestId) {}
