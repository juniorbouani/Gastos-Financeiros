package com.georgesbouanni.controle_gastos.dto;

import com.georgesbouanni.controle_gastos.model.InvoiceItem;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceEntryResponse(
        Long id,
        Long invoiceId,
        String descricao,
        BigDecimal valor,
        LocalDate data,
        int parcelaAtual,
        int totalParcelas,
        boolean estornado) {
    public static InvoiceEntryResponse from(InvoiceItem item) {
        return new InvoiceEntryResponse(
                item.getId(),
                item.getInvoice().getId(),
                item.getDescricao(),
                item.getValor(),
                item.getData(),
                item.getParcelaAtual(),
                item.getTotalParcelas(),
                item.isEstornado());
    }
}
