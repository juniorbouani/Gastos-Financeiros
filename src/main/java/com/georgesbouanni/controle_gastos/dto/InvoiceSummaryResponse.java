package com.georgesbouanni.controle_gastos.dto;

import com.georgesbouanni.controle_gastos.model.Invoice;
import com.georgesbouanni.controle_gastos.model.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record InvoiceSummaryResponse(Long id, Long cardId, YearMonth mesReferencia,
        BigDecimal valorTotal, BigDecimal valorPago, BigDecimal valorRestante,
        InvoiceStatus status, LocalDate dataVencimento) {
    public static InvoiceSummaryResponse from(Invoice invoice) {
        return new InvoiceSummaryResponse(invoice.getId(), invoice.getCreditCard().getId(),
                invoice.getMesReferencia(), invoice.getValorTotal(), invoice.getValorPago(),
                invoice.getValorTotal().subtract(invoice.getValorPago()),
                invoice.getStatus(), invoice.getDataVencimento());
    }
}
