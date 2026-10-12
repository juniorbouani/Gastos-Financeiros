package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository  extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdAndMesReferencia(Long userId, YearMonth mesReferencia);

    Optional<Budget> findByUserIdAndCategoryIdAndMesReferencia(Long userId, Long categoryId, YearMonth mesReferencia);
}
