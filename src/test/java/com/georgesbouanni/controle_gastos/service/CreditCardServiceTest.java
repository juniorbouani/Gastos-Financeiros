package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.BusinessRuleException;
import com.georgesbouanni.controle_gastos.exception.ForbiddenException;
import com.georgesbouanni.controle_gastos.model.CreditCard;
import com.georgesbouanni.controle_gastos.model.Invoice;
import com.georgesbouanni.controle_gastos.model.InvoiceItem;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.CreditCardRepository;
import com.georgesbouanni.controle_gastos.repository.InvoiceItemRepository;
import com.georgesbouanni.controle_gastos.repository.InvoiceRepository;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)

public class CreditCardServiceTest {

    @Mock
    private CreditCardRepository cardRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CreditCardService service;

    private User dono() {
        User u = new User();
        u.setId(1L);
        u.setBalance(new BigDecimal("1000.00"));
        return u;
    }

    private CreditCard cartaoDe(User dono) {
        CreditCard card = new CreditCard(dono, "**** 1234", new BigDecimal("1000.00"), false,
                LocalDate.now().plusYears(5));
        card.setId(10L);
        return card;
    }

    @Test
    void negarBloqueioDeCartaoDeOutroUsuario() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(cartaoDe(dono())));

        Assertions.assertThrows(ForbiddenException.class, () -> service.blockCard(10L, 2L));
        Mockito.verify(cardRepository, Mockito.never()).save(Mockito.any(CreditCard.class));
    }

    @Test
    void negarEstornoDuploDoMesmoLancamento() {
        CreditCard card = cartaoDe(dono());
        Invoice invoice = new Invoice(card, YearMonth.of(2026, 10), LocalDate.of(2026, 11, 10));
        invoice.setValorTotal(new BigDecimal("100.00"));
        InvoiceItem item = new InvoiceItem(invoice, "Compra", new BigDecimal("100.00"),
                LocalDate.of(2026, 10, 5), 1, 1);
        item.setEstornado(true);

        when(invoiceItemRepository.findById(7L)).thenReturn(Optional.of(item));

        Assertions.assertThrows(BusinessRuleException.class, () -> service.disputeItem(7L, 1L));
        Mockito.verify(cardRepository, Mockito.never()).save(Mockito.any(CreditCard.class));
    }

    @Test
    void negarPagamentoMaiorQueOSaldoDevedor() {
        User dono = dono();
        Invoice invoice = new Invoice(cartaoDe(dono), YearMonth.of(2026, 10), LocalDate.of(2026, 11, 10));
        invoice.setValorTotal(new BigDecimal("100.00"));

        when(invoiceRepository.findById(3L)).thenReturn(Optional.of(invoice));

        Assertions.assertThrows(BusinessRuleException.class,
                () -> service.payInvoice(3L, 1L, new BigDecimal("150.00")));
        Assertions.assertEquals(new BigDecimal("1000.00"), dono.getBalance());
    }
}
