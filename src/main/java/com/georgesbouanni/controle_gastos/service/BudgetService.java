package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.dto.BudgetSummaryResponse;
import com.georgesbouanni.controle_gastos.exception.BusinessRuleException;
import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.Budget;
import com.georgesbouanni.controle_gastos.model.Category;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.BudgetRepository;
import com.georgesbouanni.controle_gastos.repository.CategoryRepository;
import com.georgesbouanni.controle_gastos.repository.TransactionRepository;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository repository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public BudgetService(BudgetRepository repository, UserRepository userRepository,
                         CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public BudgetSummaryResponse create(Long userId, Long categoryId, YearMonth mes, BigDecimal limite) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + userId));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + categoryId));

        if (!category.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("Essa categoria não pertence ao usuário informado.");
        }

        if (repository.findByUserIdAndCategoryIdAndMesReferencia(userId, categoryId, mes).isPresent()) {
            throw new BusinessRuleException("Já existe um orçamento para essa categoria nesse mês.");
        }

        Budget budget = repository.save(new Budget(user, category, mes, limite));
        return toSummary(budget);
    }

    public List<BudgetSummaryResponse> listByUserAndMonth(Long userId, YearMonth mes) {
        return repository.findByUserIdAndMesReferencia(userId, mes).stream()
                .map(this::toSummary)
                .toList();
    }

    public BudgetSummaryResponse updateLimit(Long id, BigDecimal novoLimite) {
        Budget budget = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado com id: " + id));
        budget.setLimite(novoLimite);
        return toSummary(repository.save(budget));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private BudgetSummaryResponse toSummary(Budget budget) {
        BigDecimal gasto = transactionRepository.sumSpent(
                budget.getUser().getId(),
                budget.getCategory().getId(),
                budget.getMesReferencia().atDay(1),
                budget.getMesReferencia().atEndOfMonth()
        );

        return new BudgetSummaryResponse(
                budget.getId(),
                budget.getCategory().getName(),
                budget.getMesReferencia(),
                budget.getLimite(),
                gasto
        );
    }
}
