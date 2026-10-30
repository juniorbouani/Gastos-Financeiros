package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.ForbiddenException;
import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.*;
import com.georgesbouanni.controle_gastos.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceQueryServiceTest {
    @Mock CreditCardService cards;
    @Mock InvoiceRepository invoices;
    @Mock InvoiceItemRepository items;
    @InjectMocks InvoiceQueryService service;

    private Invoice invoice(Long id, String month) {
        CreditCard card = new CreditCard();
        card.setId(10L);
        Invoice invoice = new Invoice(card, YearMonth.parse(month), LocalDate.parse("2026-12-10"));
        invoice.setId(id);
        invoice.setValorTotal(new BigDecimal("100.00"));
        invoice.setValorPago(new BigDecimal("25.00"));
        return invoice;
    }

    @Test void rejectsForeignCardBeforeReadingInvoices() {
        when(cards.findOwnedCard(10L, 2L)).thenThrow(new ForbiddenException("Não pertence a você"));
        assertThrows(ForbiddenException.class, () -> service.list(10L, 2L));
        verifyNoInteractions(invoices, items);
    }

    @Test void rejectsForeignInvoiceBeforeReadingEntries() {
        when(invoices.findById(30L)).thenReturn(Optional.of(invoice(30L,"2026-10")));
        when(cards.findOwnedCard(10L, 2L)).thenThrow(new ForbiddenException("Não pertence a você"));
        assertThrows(ForbiddenException.class, () -> service.entries(30L, 2L));
        verifyNoInteractions(items);
    }

    @Test void missingInvoiceDoesNotQueryEntries() {
        when(invoices.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.entries(99L, 1L));
        verifyNoInteractions(items, cards);
    }

    @Test void returnsNewestInvoicesWithRemainingBalance() {
        when(invoices.findByCreditCardId(10L)).thenReturn(List.of(invoice(30L,"2026-10"),invoice(31L,"2026-11")));
        var result = service.list(10L,1L);
        assertEquals(31L,result.getFirst().id());
        assertEquals(new BigDecimal("75.00"),result.getFirst().valorRestante());
        verify(cards).findOwnedCard(10L,1L);
    }

    @Test void keepsReversedEntriesVisibleInHistory() {
        Invoice inv = invoice(30L,"2026-10");
        InvoiceItem item = new InvoiceItem(inv,"Compra",new BigDecimal("20.00"),LocalDate.parse("2026-10-10"),1,2);
        item.setId(50L);item.setEstornado(true);
        when(invoices.findById(30L)).thenReturn(Optional.of(inv));
        when(items.findByInvoiceId(30L)).thenReturn(List.of(item));
        var result=service.entries(30L,1L);
        assertEquals(1,result.size());assertTrue(result.getFirst().estornado());
        assertEquals(2,result.getFirst().totalParcelas());
        verify(cards).findOwnedCard(10L,1L);
    }
}
