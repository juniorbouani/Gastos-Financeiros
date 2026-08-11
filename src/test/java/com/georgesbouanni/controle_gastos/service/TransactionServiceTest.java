package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.model.Transaction;
import com.georgesbouanni.controle_gastos.model.TransactionType;
import com.georgesbouanni.controle_gastos.repository.TransactionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @InjectMocks
    private TransactionService service;

    @Test
    void testeListarTodasAsTransacoes() {

        Transaction transaction = new Transaction(
                null,
                "Mercado",
                new BigDecimal("150.50"),
                LocalDate.of(2026,7,20),
                TransactionType.EXPENSE,
                "Alimentação"
        );

        when(repository.findAll()).thenReturn(List.of(transaction));

        List<Transaction> resultado = service.listAll();

        Assertions.assertEquals(1, resultado.size());
        Assertions.assertEquals("Mercado", resultado.get(0).getDescription());
        Mockito.verify(repository, times(1)).findAll();

    }

    @Test
    void deveBuscarTransacaoPorId() {
        Transaction transaction = new Transaction(
                null,
                "Mercado",
                new BigDecimal("150.50"),
                LocalDate.of(2026,7,20),
                TransactionType.EXPENSE,
                "Alimentação"
        );
        transaction.setId("abc123");

        when(repository.findById("abc123")).thenReturn(Optional.of(transaction));

        Optional<Transaction> resultado = service.findById("abc123");

        Assertions.assertTrue(resultado.isPresent());
        Assertions.assertEquals("Mercado", resultado.get().getDescription());
    }

    @Test
    void deveRetornarVazioQuandoNãoTiverID() {
        when(repository.findById("id-invalido")).thenReturn(Optional.empty());

        Optional<Transaction> resultado = service.findById("id-invalido");

        Assertions.assertTrue(resultado.isEmpty());
    }

    @Test
    void deveSalvarTransacao() {
        Transaction transaction = new Transaction(
                null,
                "Mercado",
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 20),
                TransactionType.EXPENSE,
                "Alimentação"
        );

        when(repository.save(transaction)).thenReturn(transaction);

        Transaction resultado = service.save(transaction);

        Assertions.assertNotNull(resultado);
        Assertions.assertEquals("Mercado", resultado.getDescription());
        Mockito.verify(repository, times(1)).save(transaction);
    }

    @Test
    void deveDeletarTransacao() {
        service.delete("abc123");

        Mockito.verify(repository, times(1)).deleteById("abc123");
    }
}