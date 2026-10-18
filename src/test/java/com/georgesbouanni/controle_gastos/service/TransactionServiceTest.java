package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.dto.TransactionRequest;
import com.georgesbouanni.controle_gastos.exception.BusinessRuleException;
import com.georgesbouanni.controle_gastos.exception.InsuficientBalanceException;
import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.*;
import com.georgesbouanni.controle_gastos.repository.BudgetRepository;
import com.georgesbouanni.controle_gastos.repository.CategoryRepository;
import com.georgesbouanni.controle_gastos.repository.TransactionRepository;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService service;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    void listarTodasAsTransacoes() {
        User sender = new User();
        sender.setId(1L);

        User receiver = new User();
        receiver.setId(2L);

        Transaction transaction = new Transaction(
                "Pix para João",
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 20),
                TransactionType.PIX,
                TransactionStatus.COMPLETED,
                sender,
                receiver,
                null,
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
        User sender = new User();
        sender.setId(1L);

        Transaction transaction = new Transaction(
                "Pix para João",
                new BigDecimal("150.50"),
                LocalDate.of(2026, 7, 20),
                TransactionType.PIX,
                TransactionStatus.COMPLETED,
                sender,
                null,
                null,
                null
        );
        transaction.setId(10L);

        when(repository.findById(10L)).thenReturn(Optional.of(transaction));

        Optional<Transaction> resultado = service.findById(10L);

        Assertions.assertTrue(resultado.isPresent());
        Assertions.assertEquals("Pix para João", resultado.get().getDescription());
    }

    @Test
    void retornarVazioQuandoNãoTiverID() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        Optional<Transaction> resultado = service.findById(999L);

        Assertions.assertTrue(resultado.isEmpty());
    }

    @Test
    void salvarTransacaoComSaldoSuficiente() {
        User sender = new User();
        sender.setId(1L);
        sender.setBalance(new BigDecimal("500.00"));

        User receiver = new User();
        receiver.setId(2L);
        receiver.setBalance(new BigDecimal("100.00"));

        TransactionRequest request = new TransactionRequest();
        request.setDescription("Pix para João");
        request.setValue(new BigDecimal("150.50"));
        request.setDate(LocalDate.of(2026, 7, 20));
        request.setType(TransactionType.PIX);
        request.setReceiverId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(repository.save(Mockito.any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction resultado = service.save(request, 1L);

        Assertions.assertNotNull(resultado);
        Assertions.assertEquals(TransactionStatus.COMPLETED, resultado.getStatus());
        Assertions.assertEquals(new BigDecimal("349.50"), sender.getBalance());
        Assertions.assertEquals(new BigDecimal("250.50"), receiver.getBalance());
        Mockito.verify(repository, times(1)).save(Mockito.any(Transaction.class));
    }

    @Test
    void lancarExcecaoQuandoTiverSaldoInsuficiente() {
        User sender = new User();
        sender.setId(1L);
        sender.setBalance(new BigDecimal("50.00"));

        TransactionRequest request = new TransactionRequest();
        request.setDescription("Pix para João");
        request.setValue(new BigDecimal("150.50"));
        request.setDate(LocalDate.of(2026, 7, 20));
        request.setType(TransactionType.PIX);
        request.setReceiverId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));

        Assertions.assertThrows(InsuficientBalanceException.class, () -> service.save(request, 1l));
        Mockito.verify(repository, times(0)).save(Mockito.any());
    }

    @Test
    void lancarExcecaoQuandoRemetenteNaoExiste() {
        TransactionRequest request = new TransactionRequest();
        request.setDescription("Pix para João");
        request.setValue(new BigDecimal("150.50"));
        request.setDate(LocalDate.of(2026, 7, 20));
        request.setType(TransactionType.PIX);
        request.setReceiverId(2L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.save(request,999L));
    }

    @Test
    void deletarTransacao() {
        service.delete(1L);

        Mockito.verify(repository, times(1)).deleteById(1L);
    }

    @Test
    void bloquearQuandoTransacaoUltrapassaOrcamento() {
        User sender = new User();
        sender.setId(1L);
        sender.setBalance(new BigDecimal("1000.00"));

        User receiver = new User();
        receiver.setId(2L);
        receiver.setBalance(new BigDecimal("0.00"));

        Category category = new Category();
        category.setId(5L);
        category.setName("Alimentação");
        category.setUser(sender);

        Budget budget = new Budget(sender, category, YearMonth.of(2026, 10), new BigDecimal("200.00"));

        TransactionRequest request = new TransactionRequest();
        request.setDescription("Mercado");
        request.setValue(new BigDecimal("100.00"));
        request.setDate(LocalDate.of(2026, 10, 13));
        request.setType(TransactionType.TRANSFERENCIA);
        request.setReceiverId(2L);
        request.setCategoryId(5L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryIdAndMesReferencia(1L, 5L, YearMonth.of(2026, 10)))
                .thenReturn(Optional.of(budget));
        when(repository.sumSpent(1L, 5L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(new BigDecimal("150.00"));

        Assertions.assertThrows(BusinessRuleException.class, () -> service.save(request, 1L));

        Assertions.assertEquals(new BigDecimal("1000.00"), sender.getBalance());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
        Mockito.verify(repository, Mockito.never()).save(Mockito.any(Transaction.class));
    }

    @Test
    void notificarQuandoGastoAtingeOLimiteDoOrcamento() {
        User sender = new User();
        sender.setId(1L);
        sender.setBalance(new BigDecimal("1000.00"));

        User receiver = new User();
        receiver.setId(2L);
        receiver.setBalance(new BigDecimal("0.00"));

        Category category = new Category();
        category.setId(5L);
        category.setName("Alimentação");
        category.setUser(sender);

        Budget budget = new Budget(sender, category, YearMonth.of(2026, 10), new BigDecimal("200.00"));

        TransactionRequest request = new TransactionRequest();
        request.setDescription("Mercado");
        request.setValue(new BigDecimal("100.00"));
        request.setDate(LocalDate.of(2026, 10, 13));
        request.setType(TransactionType.TRANSFERENCIA);
        request.setReceiverId(2L);
        request.setCategoryId(5L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryIdAndMesReferencia(1L, 5L, YearMonth.of(2026, 10)))
                .thenReturn(Optional.of(budget));
        when(repository.sumSpent(1L, 5L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(new BigDecimal("100.00"));
        when(repository.save(Mockito.any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction resultado = service.save(request, 1L);

        Assertions.assertEquals(TransactionStatus.COMPLETED, resultado.getStatus());
        Mockito.verify(notificationService, Mockito.times(1)).create(Mockito.eq(sender), Mockito.anyString());
    }
}