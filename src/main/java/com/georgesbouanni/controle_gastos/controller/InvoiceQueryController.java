package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.dto.InvoiceSummaryResponse;
import com.georgesbouanni.controle_gastos.dto.InvoiceEntryResponse;
import com.georgesbouanni.controle_gastos.security.CurrentUserService;
import com.georgesbouanni.controle_gastos.service.InvoiceQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/** Expõe consultas de faturas e lançamentos sem devolver o grafo completo de entidades. */
@RestController
@RequestMapping("/credit-cards")
public class InvoiceQueryController {
    private final InvoiceQueryService service;
    private final CurrentUserService currentUser;

    public InvoiceQueryController(InvoiceQueryService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/{cardId}/invoices")
    public List<InvoiceSummaryResponse> list(@PathVariable Long cardId) {
        return service.list(cardId, currentUser.getId());
    }

    @GetMapping("/invoices/{invoiceId}/items")
    public List<InvoiceEntryResponse> entries(@PathVariable Long invoiceId) {
        return service.entries(invoiceId, currentUser.getId());
    }
}
