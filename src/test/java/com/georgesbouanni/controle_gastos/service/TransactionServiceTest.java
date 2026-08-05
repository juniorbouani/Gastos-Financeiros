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

}