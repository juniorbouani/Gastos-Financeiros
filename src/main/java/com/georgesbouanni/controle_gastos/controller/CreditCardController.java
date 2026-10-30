package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.dto.CreditCardRequest;
import com.georgesbouanni.controle_gastos.dto.PaymentRequest;
import com.georgesbouanni.controle_gastos.dto.PurchaseRequest;
import com.georgesbouanni.controle_gastos.dto.SpendingLimitRequest;
import com.georgesbouanni.controle_gastos.model.CreditCard;
import com.georgesbouanni.controle_gastos.model.Invoice;
import com.georgesbouanni.controle_gastos.model.InvoiceItem;
import com.georgesbouanni.controle_gastos.security.CurrentUserService;
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
    private final CurrentUserService currentUser;

    @Autowired
    public CreditCardController(CreditCardService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreditCard requestCard(@Valid @RequestBody CreditCardRequest request) {
        return service.requestCard(
                currentUser.getId(), request.getLimiteTotal(), request.isDescartavel());
    }

    @GetMapping("/user/{userId}")
    public List<CreditCard> listByUser(@PathVariable Long userId) {
        currentUser.requireSameUser(userId);
        return service.listByUser(userId);
    }

    @PutMapping("/{id}/block")
    public CreditCard blockCard(@PathVariable Long id) {
        return service.blockCard(id, currentUser.getId());
    }

    @PutMapping("/{id}/unblock")
    public CreditCard unblockCard(@PathVariable Long id) {
        return service.unblockCard(id, currentUser.getId());
    }

    @PutMapping("/{id}/limit")
    public CreditCard setSpendingLimit(
            @PathVariable Long id, @Valid @RequestBody SpendingLimitRequest request) {
        return service.setSpendingLimit(id, currentUser.getId(), request.getNovoLimite());
    }

    @PostMapping("/{id}/purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceItem registerPurchase(
            @PathVariable Long id, @Valid @RequestBody PurchaseRequest request) {
        return service.registerPurchase(
                id,
                currentUser.getId(),
                request.getDescricao(),
                request.getValor(),
                request.getTotalParcelas());
    }

    @PostMapping("/invoices/{invoiceId}/pay")
    public Invoice payInvoice(
            @PathVariable Long invoiceId, @Valid @RequestBody PaymentRequest request) {
        return service.payInvoice(invoiceId, currentUser.getId(), request.getValor());
    }

    @PutMapping("/invoice-items/{itemId}/dispute")
    public InvoiceItem disputeItem(@PathVariable Long itemId) {
        return service.disputeItem(itemId, currentUser.getId());
    }
}
