package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.dto.CreditCardRequest;
import com.georgesbouanni.controle_gastos.dto.PaymentRequest;
import com.georgesbouanni.controle_gastos.dto.PurchaseRequest;
import com.georgesbouanni.controle_gastos.dto.SpedingLimitRequest;
import com.georgesbouanni.controle_gastos.model.CreditCard;
import com.georgesbouanni.controle_gastos.model.Invoice;
import com.georgesbouanni.controle_gastos.model.InvoiceItem;
import com.georgesbouanni.controle_gastos.service.CreditCardService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/credit-cards")
public class CreditCardController {

    private final CreditCardService service;

    @Autowired
    public CreditCardController(CreditCardService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreditCard requestCard(@Valid @RequestBody CreditCardRequest request) {
        return service.requestCard(request.getUserId(), request.getLimiteTotal(), request.isDescartavel());
    }

    @GetMapping("/user/{userId}")
    public List<CreditCard> listByUser(@PathVariable Long userId) {
        return service.listByUser(userId);
    }

    @PutMapping("/{id}/block")
    public CreditCard blockCard(@PathVariable Long id) {
        return service.blockCard(id);
    }

    @PutMapping("/{id}/unblock")
    public CreditCard unBlockCard(@PathVariable Long id) {
        return service.unblockCard(id);
    }

    @PutMapping("/{id}/limit")
    public CreditCard setSpendingLimit(@PathVariable Long id, @Valid @RequestBody SpedingLimitRequest request) {
        return service.setSpendingLimit(id, request.getNovoLimite());
    }

    @PostMapping("/{id}/purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceItem registerPurchase(@PathVariable Long id, @Valid @RequestBody PurchaseRequest request) {
        return service.registerPurchase(id, request.getDescricao(), request.getValor(), request.getTotalParcelas());
    }

    @PostMapping("/invoices/{invoiceId}/pay")
    public Invoice payInvoice(@PathVariable Long invoiceId, @Valid @RequestBody PaymentRequest request) {
        return service.payInvoice(invoiceId, request.getValor());
    }

    @PutMapping("/invoice-items/{itemId}/dispute")
    public InvoiceItem disputeItem(@PathVariable Long itemId) {
        return service.disputeItem(itemId);
    }
}