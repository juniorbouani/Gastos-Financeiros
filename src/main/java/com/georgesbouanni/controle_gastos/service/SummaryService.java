package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.dto.CategorySpent;
import com.georgesbouanni.controle_gastos.dto.CategorySummaryItem;
import com.georgesbouanni.controle_gastos.dto.MonthlySummaryResponse;
import com.georgesbouanni.controle_gastos.model.Budget;
import com.georgesbouanni.controle_gastos.repository.BudgetRepository;
import com.georgesbouanni.controle_gastos.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SummaryService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    @Autowired
    public SummaryService(
            TransactionRepository transactionRepository, BudgetRepository budgetRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }

    public MonthlySummaryResponse monthly(Long userId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<CategorySpent> rows = transactionRepository.spentByCategory(userId, start, end);
        BigDecimal received = transactionRepository.sumReceived(userId, start, end);

        Map<Long, BigDecimal> limits =
                budgetRepository.findByUserIdAndMesReferencia(userId, month).stream()
                        .collect(Collectors.toMap(b -> b.getCategory().getId(), Budget::getLimite));

        BigDecimal totalSpent =
                rows.stream().map(CategorySpent::total).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategorySummaryItem> items =
                rows.stream()
                        .sorted(
                                Comparator.comparing(
                                        CategorySpent::total, Comparator.reverseOrder()))
                        .map(row -> toItem(row, totalSpent, limits))
                        .toList();

        return new MonthlySummaryResponse(month, totalSpent, received, items);
    }

    private CategorySummaryItem toItem(
            CategorySpent row, BigDecimal totalSpent, Map<Long, BigDecimal> limits) {
        BigDecimal percentage =
                totalSpent.signum() == 0
                        ? BigDecimal.ZERO
                        : row.total()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(totalSpent, 1, RoundingMode.HALF_UP);

        BigDecimal limit = row.categoryId() != null ? limits.get(row.categoryId()) : null;
        BigDecimal remaining = limit != null ? limit.subtract(row.total()) : null;
        String name = row.categoryName() != null ? row.categoryName() : "Sem categoria";

        return new CategorySummaryItem(
                row.categoryId(), name, row.total(), row.count(), percentage, limit, remaining);
    }
}
