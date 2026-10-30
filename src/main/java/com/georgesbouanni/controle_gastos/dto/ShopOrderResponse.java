package com.georgesbouanni.controle_gastos.dto;

import com.georgesbouanni.controle_gastos.model.ShopOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Resumo de pedido sem senha, número completo de cartão ou dados pessoais do usuário. */
public record ShopOrderResponse(
        Long id,
        String requestId,
        String productId,
        String produto,
        BigDecimal total,
        int totalParcelas,
        Long cardId,
        String cartao,
        Long firstInvoiceId,
        LocalDateTime createdAt) {
    public static ShopOrderResponse from(ShopOrder order) {
        return new ShopOrderResponse(
                order.getId(),
                order.getRequestId(),
                order.getProductId(),
                order.getProductName(),
                order.getTotal(),
                order.getTotalParcelas(),
                order.getCard().getId(),
                order.getCard().getNumeroMascarado(),
                order.getFirstInvoiceId(),
                order.getCreatedAt());
    }
}
