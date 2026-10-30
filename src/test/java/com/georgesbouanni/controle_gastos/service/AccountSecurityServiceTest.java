package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.dto.*;
import com.georgesbouanni.controle_gastos.exception.*;
import com.georgesbouanni.controle_gastos.model.*;
import com.georgesbouanni.controle_gastos.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountSecurityServiceTest {
    @Mock UserRepository users;
    @Mock CreditCardRepository cards;
    @Mock InvoiceRepository invoices;
    @Mock PixKeyRepository keys;
    @Mock PasswordEncoder encoder;
    @Mock LoginAttemptService attempts;
    @Mock NotificationService notifications;
    @InjectMocks AccountSecurityService service;

    User user() {
        User u = new User();
        u.setId(7L);
        u.setCpf("12345678900");
        u.setSenhaHash("hash-antigo");
        u.setBalance(BigDecimal.ZERO);
        return u;
    }

    User authenticated() {
        User u = user();
        when(users.findById(7L)).thenReturn(Optional.of(u));
        when(encoder.matches("atual", "hash-antigo")).thenReturn(true);
        return u;
    }

    @Test
    void wrongPasswordDoesNotChangeCredentials() {
        User u = user();
        when(users.findById(7L)).thenReturn(Optional.of(u));
        assertThrows(
                BusinessRuleException.class,
                () ->
                        service.changePassword(
                                7L, new PasswordChangeRequest("errada", "novaSenha123")));
        verify(attempts).registerFailure(u.getCpf());
        verify(users, never()).save(any());
        verifyNoInteractions(cards, keys);
        assertEquals(0, u.getCredentialVersion());
    }

    @Test
    void passwordChangeHashesAndRevokesPreviousTokens() {
        User u = authenticated();
        when(encoder.encode("novaSenha123")).thenReturn("novo-hash");
        service.changePassword(7L, new PasswordChangeRequest("atual", "novaSenha123"));
        assertEquals("novo-hash", u.getSenhaHash());
        assertEquals(1, u.getCredentialVersion());
        verify(users).save(u);
        verify(notifications).create(eq(u), anyString());
    }

    @Test
    void rejectsSamePassword() {
        User u = user();
        when(users.findById(7L)).thenReturn(Optional.of(u));
        when(encoder.matches("senhaAtual123", "hash-antigo")).thenReturn(true);
        assertThrows(
                BusinessRuleException.class,
                () ->
                        service.changePassword(
                                7L, new PasswordChangeRequest("senhaAtual123", "senhaAtual123")));
        verify(users, never()).save(any());
    }

    @Test
    void rejectsPasswordOverBcryptByteLimit() {
        authenticated();
        assertThrows(
                BusinessRuleException.class,
                () ->
                        service.changePassword(
                                7L, new PasswordChangeRequest("atual", "😀".repeat(20))));
        verify(users, never()).save(any());
    }

    @Test
    void closureRequiresZeroBalance() {
        User u = authenticated();
        u.setBalance(BigDecimal.ONE);
        assertThrows(
                BusinessRuleException.class,
                () -> service.closeAccount(7L, new AccountCloseRequest("atual", "ENCERRAR")));
        assertFalse(u.isAccountClosed());
        verifyNoInteractions(cards, keys);
    }

    @Test
    void closureRequiresExplicitConfirmation() {
        authenticated();
        assertThrows(
                BusinessRuleException.class,
                () -> service.closeAccount(7L, new AccountCloseRequest("atual", "sim")));
        verify(users, never()).save(any());
    }

    @Test
    void closureRejectsFutureInvoiceDebtBeforeChangingCards() {
        User u = authenticated();
        CreditCard c = new CreditCard();
        c.setId(10L);
        c.setStatus(CreditCardStatus.ATIVO);
        Invoice inv = new Invoice(c, YearMonth.now().plusMonths(2), LocalDate.now().plusMonths(3));
        inv.setValorTotal(new BigDecimal("50.00"));
        when(cards.findByUserId(7L)).thenReturn(List.of(c));
        when(invoices.findByCreditCardId(10L)).thenReturn(List.of(inv));
        assertThrows(
                BusinessRuleException.class,
                () -> service.closeAccount(7L, new AccountCloseRequest("atual", "ENCERRAR")));
        assertFalse(u.isAccountClosed());
        assertEquals(CreditCardStatus.ATIVO, c.getStatus());
        verifyNoInteractions(keys);
    }

    @Test
    void closurePreservesUserAndCancelsCardsAndKeys() {
        User u = authenticated();
        CreditCard c = new CreditCard();
        c.setId(10L);
        c.setStatus(CreditCardStatus.ATIVO);
        when(cards.findByUserId(7L)).thenReturn(List.of(c));
        when(invoices.findByCreditCardId(10L)).thenReturn(List.of());
        when(keys.findByUserId(7L)).thenReturn(List.of());
        service.closeAccount(7L, new AccountCloseRequest("atual", "ENCERRAR"));
        assertTrue(u.isAccountClosed());
        assertEquals(1, u.getCredentialVersion());
        assertEquals(CreditCardStatus.CANCELADO, c.getStatus());
        verify(keys).deleteAll(anyList());
        verify(users).save(u);
        verify(users, never()).delete(any());
        assertEquals("12345678900", u.getCpf());
    }

    @Test
    void closedUserCannotChangePassword() {
        User u = user();
        u.setAccountClosed(true);
        when(users.findById(7L)).thenReturn(Optional.of(u));
        assertThrows(
                ForbiddenException.class,
                () ->
                        service.changePassword(
                                7L, new PasswordChangeRequest("atual", "novaSenha123")));
        verifyNoInteractions(encoder, attempts);
    }
}
