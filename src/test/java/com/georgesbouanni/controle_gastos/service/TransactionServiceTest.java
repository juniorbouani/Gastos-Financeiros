package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.InsuficientBalanceException;
import com.georgesbouanni.controle_gastos.model.Transaction;
import com.georgesbouanni.controle_gastos.model.TransactionStatus;
import com.georgesbouanni.controle_gastos.model.TransactionType;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.TransactionRepository;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.times;

import java.lang.module.ResolutionException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService service;

    @Test
    void listarTodasAsTransacoes() {
        Transaction transaction = new Transaction(
                "Pix para João",
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 20),
                TransactionType.PIX,
                TransactionStatus.COMPLETED,
                "joãozinho234",
                "matheuzinho876",
                null
        );

        when(repository.findAll()).thenReturn(List.of(transaction));

        List<Transaction> resultado = service.listAll();

        Assertions.assertEquals(1, resultado.size());
        Assertions.assertEquals("Pix para João", resultado.get(0).getDescription());
        Mockito.verify(repository, times(1)).findAll();
    }

    @Test
    void buscarTransacaoPorId() {
        Transaction transaction = new Transaction(
                "Pix para João",
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 20),
                TransactionType.PIX,
                TransactionStatus.COMPLETED,
                "joãozinho234",
                "matheuzinho876",
                null
        );

        transaction.setId("abc123");

        when(repository.findById("abc123")).thenReturn(Optional.of(transaction));

        Optional<Transaction> resultado = service.findById("abc123");

        Assertions.assertTrue(resultado.isPresent());
        Assertions.assertEquals("Pix para João", resultado.get().getDescription());

    }

    @Test
    void retornarVazioQuandoNãoTiverID() {
        when(repository.findById("id-invalido")).thenReturn(Optional.empty());

        Optional<Transaction> resultado = service.findById("id-invalido");

        Assertions.assertTrue(resultado.isEmpty());
    }

    @Test
    void salvarTransacaoComSaldoSuficiente() {
        User sender = new User();
        sender.setId("sender123");
        sender.setBalance(new BigDecimal("500.00"));

        User receiver = new User();
        receiver.setId("receiver543");
        receiver.setBalance(new BigDecimal("100.00"));

        Transaction transaction = new Transaction(
                "Pix para João",
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 20),
                TransactionType.PIX,
                TransactionStatus.COMPLETED,
                "joãozinho234",
                "matheuzinho876",
                null
        );

        when(userRepository.findById("joãozinho234")).thenReturn(Optional.of(sender));
        when(userRepository.findById("receiver543")).thenReturn(Optional.of(receiver));
        when(repository.save(transaction)).thenReturn(transaction);

        Transaction resultado = service.save(transaction);

        Assertions.assertNotNull(resultado);
        Assertions.assertEquals(TransactionStatus.COMPLETED, resultado.getStatus());
        Assertions.assertEquals(new BigDecimal("349.50"), sender.getBalance());
        Assertions.assertEquals(new BigDecimal("250.50"), receiver.getBalance());
        Mockito.verify(repository, times(1)).save(transaction);
    }

    @Test
    void lancarExcecaoQuandoTiverSaldoInsuficiente() {
        User sender = new User();
        sender.setId("sender123");
        sender.setBalance(new BigDecimal("50.00"));

        Transaction transaction = new Transaction(
                "Pix para João",
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 20),
                TransactionType.PIX,
                TransactionStatus.COMPLETED,
                "joãozinho234",
                "matheuzinho876",
                null
        );

        when(userRepository.findById("joãozinho234")).thenReturn(Optional.of(sender));

        Assertions.assertThrows(InsuficientBalanceException.class, () -> service.save(transaction));
        Mockito.verify(repository, times(0)).save(Mockito.any());
    }

    @Test
    void lancarExcecaoQuandoRemetenteNaoExiste() {
        Transaction transaction = new Transaction(
                "Pix para João",
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 20),
                TransactionType.PIX,
                null,
                "sender-inexistente",
                "matheuzinho876",
                null
        );

        when(userRepository.findById("sender-inexistente")).thenReturn(Optional.empty());

        Assertions.assertThrows(ResolutionException.class, () -> service.save(transaction));
    }

    @Test
    void deletarTransacao() {
        service.delete("abc123");

        Mockito.verify(repository, times(1)).deleteById("abc123");
    }

}