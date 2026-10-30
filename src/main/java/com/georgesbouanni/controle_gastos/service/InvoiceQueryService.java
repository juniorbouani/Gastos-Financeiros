package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.dto.InvoiceSummaryResponse;
import com.georgesbouanni.controle_gastos.dto.InvoiceEntryResponse;
import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.Invoice;
import com.georgesbouanni.controle_gastos.repository.InvoiceRepository;
import com.georgesbouanni.controle_gastos.repository.InvoiceItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;

/** Confere a propriedade do cartão antes de consultar faturas ou lançamentos. */
@Service
@Transactional(readOnly = true)
public class InvoiceQueryService {

    private final CreditCardService cards;
    private final InvoiceRepository invoices;
    private final InvoiceItemRepository items;

    public InvoiceQueryService(
            CreditCardService cards, InvoiceRepository invoices, InvoiceItemRepository items) {
        this.cards = cards;
        this.invoices = invoices;
        this.items = items;
    }

    public List<InvoiceSummaryResponse> list(Long cardId, Long userId) {
        cards.findOwnedCard(cardId, userId);
        return invoices.findByCreditCardId(cardId).stream()
                .sorted(Comparator.comparing(Invoice::getMesReferencia).reversed())
                .map(InvoiceSummaryResponse::from)
                .toList();
    }

    public List<InvoiceEntryResponse> entries(Long invoiceId, Long userId) {
        Invoice invoice =
                invoices.findById(invoiceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada."));
        cards.findOwnedCard(invoice.getCreditCard().getId(), userId);
        return items.findByInvoiceId(invoiceId).stream()
                .map(InvoiceEntryResponse::from)
                .sorted(
                        Comparator.comparing(InvoiceEntryResponse::data)
                                .thenComparing(InvoiceEntryResponse::id))
                .toList();
    }
}
