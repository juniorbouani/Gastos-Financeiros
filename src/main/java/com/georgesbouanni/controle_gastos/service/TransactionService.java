package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.dto.TransactionRequest;
import com.georgesbouanni.controle_gastos.exception.BusinessRuleException;
import com.georgesbouanni.controle_gastos.exception.InsuficientBalanceException;
import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.*;
import com.georgesbouanni.controle_gastos.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final UserRepository userRepository;
    private final PixKeyRepository pixKeyRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final NotificationService notificationService;

    @Autowired
    public TransactionService(TransactionRepository repository, UserRepository userRepository, PixKeyRepository pixKeyRepository, CategoryRepository categoryRepository, BudgetRepository budgetRepository, NotificationService notificationService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.pixKeyRepository = pixKeyRepository;
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
        this.notificationService = notificationService;
    }

    public List<Transaction> listAll() {
        return repository.findAll();
    }

    public Page<Transaction> listAllPaginated(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Optional<Transaction> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Transaction save(TransactionRequest request, Long senderId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Remetente não encontrado com id: " + senderId));

        if (sender.getBalance().compareTo(request.getValue()) < 0) {
            throw new InsuficientBalanceException("Saldo insuficiente para realizar essa transação");
        }

        User receiver = null;
        if (request.getType() == TransactionType.PIX && request.getPixKey() != null) {
            PixKey pixKey = pixKeyRepository.findByKeyValue(request.getPixKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Chave PIX não encontrada: " + request.getPixKey()));
            receiver = pixKey.getUser();
        } else if (request.getReceiverId() != null) {
            receiver = userRepository.findById(request.getReceiverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destinatario não encontrado com id: " + request.getReceiverId()));
        }

        Category category = null;
        Budget budget = null;
        BigDecimal totalAposTransacao = null;
        YearMonth mes = YearMonth.from(request.getDate());

        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + request.getCategoryId()));

            if (!category.getUser().getId().equals(sender.getId())) {
                throw new BusinessRuleException("Essa categoria não pertence ao remetente.");
            }

            budget = budgetRepository
                    .findByUserIdAndCategoryIdAndMesReferencia(sender.getId(), category.getId(), mes)
                    .orElse(null);

            if (budget != null) {
                BigDecimal gastoAtual = repository.sumSpent(
                        sender.getId(), category.getId(), mes.atDay(1), mes.atEndOfMonth());
                totalAposTransacao = gastoAtual.add(request.getValue());

                if (totalAposTransacao.compareTo(budget.getLimite()) > 0) {
                    throw new BusinessRuleException(
                            "Orçamento de " + category.getName() + " em " + mes + " excedido. Limite: "
                                    + budget.getLimite() + ", já gasto: " + gastoAtual
                                    + ", esta transação: " + request.getValue() + ".");
                }
            }
        }

        sender.setBalance(sender.getBalance().subtract(request.getValue()));
        userRepository.save(sender);

        if (receiver != null) {
            receiver.setBalance(receiver.getBalance().add(request.getValue()));
            userRepository.save(receiver);
        }

        Transaction transaction = new Transaction(
                request.getDescription(),
                request.getValue(),
                request.getDate(),
                request.getType(),
                TransactionStatus.COMPLETED,
                sender,
                receiver,
                request.getDestination(),
                category
        );

        Transaction saved = repository.save(transaction);

        if (budget != null && totalAposTransacao.compareTo(budget.getLimite()) == 0) {
            notificationService.create(sender,
                    "Você atingiu 100% do orçamento de " + category.getName() + " em " + mes + ".");
        }

        return saved;
    }

    public Transaction update(Long id, Transaction updatedData) {
        Transaction existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada com id: " + id));

        existing.setDescription(updatedData.getDescription());
        existing.setValue(updatedData.getValue());
        existing.setDate(updatedData.getDate());
        existing.setType(updatedData.getType());
        existing.setDestination(updatedData.getDestination());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<Transaction> listByType(TransactionType type) {
        return repository.findByType(type);
    }

    public List<Transaction> listByPeriod(LocalDate start, LocalDate end) {
        return repository.findByDateBetween(start, end);
    }

    public List<Transaction> listByUser(Long userId) {
        return repository.findBySenderIdOrReceiverId(userId, userId);
    }
}