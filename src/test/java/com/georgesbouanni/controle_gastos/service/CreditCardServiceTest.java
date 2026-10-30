package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.BusinessRuleException;
import com.georgesbouanni.controle_gastos.exception.ForbiddenException;
import com.georgesbouanni.controle_gastos.model.*;
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

    @Mock
    private NotificationService notificationService;

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

    @Test
    void pagarFaturaNotificaOUsuario() {
        User dono = dono();
        Invoice invoice = new Invoice(cartaoDe(dono), YearMonth.of(2026, 10), LocalDate.of(2026, 11, 10));
        invoice.setValorTotal(new BigDecimal("100.00"));
        when(invoiceRepository.findById(3L)).thenReturn(Optional.of(invoice));

        service.payInvoice(3L, 1L, new BigDecimal("40.00"));

        Assertions.assertEquals(new BigDecimal("960.00"), dono.getBalance());
        Mockito.verify(notificationService).create(Mockito.eq(dono), Mockito.contains("R$ 40,00"));
    }

    @Test
    void bloquearCartaoNotificaOUsuario() {
        User dono = dono();
        CreditCard card = cartaoDe(dono);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        CreditCard resultado = service.blockCard(10L, 1L);

        Assertions.assertEquals(CreditCardStatus.BLOQUEADO, resultado.getStatus());
        Mockito.verify(notificationService).create(Mockito.eq(dono), Mockito.contains("bloqueado"));
    }

    @Test
    void bloquearCartaoJaBloqueadoNaoGravaNemNotifica() {
        CreditCard card = cartaoDe(dono());
        card.setStatus(CreditCardStatus.BLOQUEADO);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        service.blockCard(10L, 1L);

        Mockito.verify(cardRepository, Mockito.never()).save(Mockito.any(CreditCard.class));
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void negarDesbloqueioDeCartaoCancelado() {
        CreditCard card = cartaoDe(dono());
        card.setStatus(CreditCardStatus.CANCELADO);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        Assertions.assertThrows(BusinessRuleException.class, () -> service.unblockCard(10L, 1L));
        Assertions.assertEquals(CreditCardStatus.CANCELADO, card.getStatus());
    }

    @Test
    void alterarLimiteNotificaComValorAntigoENovo() {
        User dono = dono();
        CreditCard card = cartaoDe(dono);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        service.setSpendingLimit(10L, 1L, new BigDecimal("2000.00"));

        Assertions.assertEquals(new BigDecimal("2000.00"), card.getLimiteDisponivel());
        Mockito.verify(notificationService).create(Mockito.eq(dono),
                Mockito.argThat(msg -> msg.contains("R$ 1.000,00") && msg.contains("R$ 2.000,00")));
    }

    @Test
    void recusarLimiteAbaixoDoValorUtilizado() {
        CreditCard card = cartaoDe(dono());
        card.setLimiteDisponivel(new BigDecimal("400.00")); // 600 já utilizados
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        Assertions.assertThrows(BusinessRuleException.class,
                () -> service.setSpendingLimit(10L, 1L, new BigDecimal("500.00")));
        Mockito.verify(cardRepository, Mockito.never()).save(Mockito.any(CreditCard.class));
    }

    @Test
    void parcelasPreservamTotalDaCompraComCentavosRestantesNaUltima() {
        var card = cartaoDe(dono());
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(invoiceRepository.findByCreditCardIdAndMesReferencia(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
        when(invoiceRepository.save(org.mockito.ArgumentMatchers.any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var captor = org.mockito.ArgumentCaptor.forClass(InvoiceItem.class);
        service.registerPurchase(10L, 1L, "Compra", new BigDecimal("100.00"), 3);
        Mockito.verify(invoiceItemRepository, Mockito.times(3)).save(captor.capture());
        var valores = captor.getAllValues().stream().map(InvoiceItem::getValor).toList();
        Assertions.assertEquals(java.util.List.of(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34")), valores);
        Assertions.assertEquals(new BigDecimal("900.00"), card.getLimiteDisponivel());
    }

    @Test
    void rejeitaCompraComParcelaMenorQueUmCentavoAntesDeDebitarLimite() {
        var card = cartaoDe(dono());
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        Assertions.assertThrows(BusinessRuleException.class,
                () -> service.registerPurchase(10L, 1L, "Compra", new BigDecimal("0.01"), 2));
        Assertions.assertEquals(new BigDecimal("1000.00"), card.getLimiteDisponivel());
        Mockito.verifyNoInteractions(invoiceRepository, invoiceItemRepository);
    }
}
