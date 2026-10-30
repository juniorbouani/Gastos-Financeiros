package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.dto.TransactionRequest;
import com.georgesbouanni.controle_gastos.exception.BusinessRuleException;
import com.georgesbouanni.controle_gastos.exception.ForbiddenException;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock private TransactionRepository repository;

    @Mock private UserRepository userRepository;

    @Mock private CategoryRepository categoryRepository;

    @InjectMocks private TransactionService service;

    @Mock private BudgetRepository budgetRepository;

    @Mock private NotificationService notificationService;

    @Test
    void listarTodasAsTransacoes() {
        User sender = new User();
        sender.setId(1L);

        User receiver = new User();
        receiver.setId(2L);

        Transaction transaction =
                new Transaction(
                        "Pix para João",
                        new BigDecimal("150.50"),
                        LocalDate.of(2026, 7, 20),
                        TransactionType.PIX,
                        TransactionStatus.COMPLETED,
                        sender,
                        receiver,
                        null,
                        null);

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

        Transaction transaction =
                new Transaction(
                        "Pix para João",
                        new BigDecimal("150.50"),
                        LocalDate.of(2026, 7, 20),
                        TransactionType.PIX,
                        TransactionStatus.COMPLETED,
                        sender,
                        null,
                        null,
                        null);
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
        when(repository.save(Mockito.any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction resultado = service.save(request, 1L);

        Assertions.assertNotNull(resultado);
        Assertions.assertEquals(TransactionStatus.COMPLETED, resultado.getStatus());
        Assertions.assertEquals(new BigDecimal("349.50"), sender.getBalance());
        Assertions.assertEquals(new BigDecimal("250.50"), receiver.getBalance());
        Mockito.verify(repository, times(1)).save(Mockito.any(Transaction.class));
        Mockito.verify(notificationService).notifyTransfer(resultado);
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

        Assertions.assertThrows(ResourceNotFoundException.class, () -> service.save(request, 999L));
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

        Budget budget =
                new Budget(sender, category, YearMonth.of(2026, 10), new BigDecimal("200.00"));

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
        when(budgetRepository.findByUserIdAndCategoryIdAndMesReferencia(
                        1L, 5L, YearMonth.of(2026, 10)))
                .thenReturn(Optional.of(budget));
        when(repository.sumSpent(1L, 5L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(new BigDecimal("150.00"));

        Assertions.assertThrows(BusinessRuleException.class, () -> service.save(request, 1L));

        Assertions.assertEquals(new BigDecimal("1000.00"), sender.getBalance());
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
        Mockito.verify(repository, Mockito.never()).save(Mockito.any(Transaction.class));
        Mockito.verifyNoInteractions(notificationService);
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

        Budget budget =
                new Budget(sender, category, YearMonth.of(2026, 10), new BigDecimal("200.00"));

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
        when(budgetRepository.findByUserIdAndCategoryIdAndMesReferencia(
                        1L, 5L, YearMonth.of(2026, 10)))
                .thenReturn(Optional.of(budget));
        when(repository.sumSpent(1L, 5L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(new BigDecimal("100.00"));
        when(repository.save(Mockito.any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction resultado = service.save(request, 1L);

        Assertions.assertEquals(TransactionStatus.COMPLETED, resultado.getStatus());
        Mockito.verify(notificationService, Mockito.times(1))
                .create(Mockito.eq(sender), Mockito.anyString());
    }

    @Test
    void negarAcessoATransacaoDeOutroUsuario() {
        User sender = new User();
        sender.setId(1L);
        User receiver = new User();
        receiver.setId(2L);

        Transaction transaction =
                new Transaction(
                        "Pix",
                        new BigDecimal("10.00"),
                        LocalDate.of(2026, 10, 1),
                        TransactionType.PIX,
                        TransactionStatus.COMPLETED,
                        sender,
                        receiver,
                        null,
                        null);
        transaction.setId(10L);

        when(repository.findById(10L)).thenReturn(Optional.of(transaction));

        Assertions.assertThrows(ForbiddenException.class, () -> service.findByIdForUser(10L, 3L));
        Assertions.assertEquals(transaction, service.findByIdForUser(10L, 2L));
    }

    private User usuario(Long id, String saldo) {
        User u = new User();
        u.setId(id);
        u.setBalance(new BigDecimal(saldo));
        return u;
    }

    private Transaction transacaoOriginal(User payer, User payee) {
        Transaction t =
                new Transaction(
                        "Pix",
                        new BigDecimal("150.00"),
                        LocalDate.of(2026, 10, 22),
                        TransactionType.PIX,
                        TransactionStatus.COMPLETED,
                        payer,
                        payee,
                        null,
                        null);
        t.setId(10L);
        return t;
    }

    @Test
    void estornarDevolveOValorEGeraLancamentoDeEstorno() {
        User payer = usuario(1L, "350.00");
        User payee = usuario(2L, "250.00");
        Transaction original = transacaoOriginal(payer, payee);

        when(repository.findById(10L)).thenReturn(Optional.of(original));
        when(repository.save(Mockito.any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction estorno = service.reverse(10L, 1L);

        Assertions.assertEquals(TransactionType.ESTORNO, estorno.getType());
        Assertions.assertEquals(original, estorno.getReversalOf());
        Assertions.assertEquals(payee, estorno.getSender());
        Assertions.assertEquals(payer, estorno.getReceiver());
        Assertions.assertEquals(new BigDecimal("500.00"), payer.getBalance());
        Assertions.assertEquals(new BigDecimal("100.00"), payee.getBalance());
        Mockito.verify(notificationService).notifyReversal(estorno);
    }

    @Test
    void negarEstornoDeQuemNaoEnviou() {
        Transaction original = transacaoOriginal(usuario(1L, "0.00"), usuario(2L, "500.00"));
        when(repository.findById(10L)).thenReturn(Optional.of(original));

        Assertions.assertThrows(ForbiddenException.class, () -> service.reverse(10L, 2L));
    }

    @Test
    void negarEstornoDuplicado() {
        Transaction original = transacaoOriginal(usuario(1L, "0.00"), usuario(2L, "500.00"));
        when(repository.findById(10L)).thenReturn(Optional.of(original));
        when(repository.existsByReversalOfId(10L)).thenReturn(true);

        Assertions.assertThrows(BusinessRuleException.class, () -> service.reverse(10L, 1L));
    }

    @Test
    void negarEstornoForaDoPrazo() {
        User payer = usuario(1L, "0.00");
        User payee = usuario(2L, "500.00");
        Transaction original = transacaoOriginal(payer, payee);
        original.setCreatedAt(LocalDateTime.now().minusHours(2));
        when(repository.findById(10L)).thenReturn(Optional.of(original));

        Assertions.assertThrows(BusinessRuleException.class, () -> service.reverse(10L, 1L));
        Assertions.assertEquals(new BigDecimal("500.00"), payee.getBalance());
    }

    @Test
    void negarEstornoQuandoDestinatarioNaoTemSaldo() {
        Transaction original = transacaoOriginal(usuario(1L, "0.00"), usuario(2L, "10.00"));
        when(repository.findById(10L)).thenReturn(Optional.of(original));

        Assertions.assertThrows(InsuficientBalanceException.class, () -> service.reverse(10L, 1L));
    }

    @Test
    void contaEncerradaNaoPodeReceberTransferencia() {
        User sender = new User();
        sender.setId(1L);
        sender.setBalance(new BigDecimal("100.00"));
        User receiver = new User();
        receiver.setId(2L);
        receiver.setBalance(BigDecimal.ZERO);
        receiver.setAccountClosed(true);
        TransactionRequest request = new TransactionRequest();
        request.setValue(new BigDecimal("10.00"));
        request.setType(TransactionType.TRANSFERENCIA);
        request.setReceiverId(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        Assertions.assertThrows(BusinessRuleException.class, () -> service.save(request, 1L));
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
        Mockito.verify(repository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void estornoNaoReabreContaEncerrada() {
        User payer = new User();
        payer.setId(1L);
        payer.setBalance(BigDecimal.ZERO);
        payer.setAccountClosed(true);
        User payee = new User();
        payee.setId(2L);
        payee.setBalance(new BigDecimal("100.00"));
        Transaction original =
                new Transaction(
                        "Pix",
                        new BigDecimal("10.00"),
                        LocalDate.now(),
                        TransactionType.PIX,
                        TransactionStatus.COMPLETED,
                        payer,
                        payee,
                        null,
                        null);
        original.setId(10L);
        original.setCreatedAt(LocalDateTime.now());
        when(repository.findById(10L)).thenReturn(Optional.of(original));
        Assertions.assertThrows(BusinessRuleException.class, () -> service.reverse(10L, 1L));
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
        Assertions.assertTrue(payer.isAccountClosed());
    }
}
