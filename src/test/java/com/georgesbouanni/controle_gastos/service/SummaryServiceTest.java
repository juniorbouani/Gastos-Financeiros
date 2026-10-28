package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.dto.CategorySpent;
import com.georgesbouanni.controle_gastos.dto.CategorySummaryItem;
import com.georgesbouanni.controle_gastos.dto.MonthlySummaryResponse;
import com.georgesbouanni.controle_gastos.model.Budget;
import com.georgesbouanni.controle_gastos.model.Category;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.BudgetRepository;
import com.georgesbouanni.controle_gastos.repository.TransactionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SummaryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private SummaryService service;

    private final YearMonth mes = YearMonth.of(2026, 10);
    private final LocalDate inicio = LocalDate.of(2026, 10, 1);
    private final LocalDate fim = LocalDate.of(2026,10,31);

    @Test
    void agruparGastosPorCategoriaComPercentualEOrcamento() {
        when(transactionRepository.spentByCategory(1L, inicio, fim)).thenReturn(List.of(
                new CategorySpent(null, null, new BigDecimal("100.00"), 1L),
                new CategorySpent(2L, "Alimentação", new BigDecimal("300.00"), 3L)));
        when(transactionRepository.sumReceived(1L, inicio, fim)).thenReturn(new BigDecimal("500.00"));

        Category categoria = new Category();
        categoria.setId(2L);
        Budget orcamento = new Budget(new User(), categoria, mes, new BigDecimal("400.00"));
        when(budgetRepository.findByUserIdAndMesReferencia(1L, mes)).thenReturn(List.of(orcamento));

        MonthlySummaryResponse resumo = service.monthly(1L, mes);

        Assertions.assertEquals(new BigDecimal("400.00"), resumo.getTotalSpent());
        Assertions.assertEquals(new BigDecimal("500.00"), resumo.getTotalReceived());
        Assertions.assertEquals(new BigDecimal("100.00"), resumo.getNet());
        Assertions.assertEquals(2, resumo.getCategories().size());

        CategorySummaryItem primeira = resumo.getCategories().get(0);
        Assertions.assertEquals("Alimentação", primeira.getCategoryName());
        Assertions.assertEquals(new BigDecimal("75.0"), primeira.getPercentage());
        Assertions.assertEquals(new BigDecimal("400.00"), primeira.getBudgetLimit());
        Assertions.assertEquals(new BigDecimal("100.00"), primeira.getBudgetRemaining());

        CategorySummaryItem segunda = resumo.getCategories().get(1);
        Assertions.assertEquals("Sem categoria", segunda.getCategoryName());
        Assertions.assertNull(segunda.getBudgetLimit());
    }

    @Test
    void mesSemMovimentoDevolveResumoZerado() {
        when(transactionRepository.sumReceived(1L, inicio, fim)).thenReturn(BigDecimal.ZERO);

        MonthlySummaryResponse resumo = service.monthly(1L, mes);

        Assertions.assertEquals(BigDecimal.ZERO, resumo.getTotalSpent());
        Assertions.assertTrue(resumo.getCategories().isEmpty());
    }
}
