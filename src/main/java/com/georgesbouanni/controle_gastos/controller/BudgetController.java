package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.dto.BudgetLimitRequest;
import com.georgesbouanni.controle_gastos.dto.BudgetRequest;
import com.georgesbouanni.controle_gastos.dto.BudgetSummaryResponse;
import com.georgesbouanni.controle_gastos.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/budgets")
public class BudgetController {

    private final BudgetService service;

    @Autowired
    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetSummaryResponse create(@Valid @RequestBody BudgetRequest request) {
        return service.create(request.getUserId(), request.getCategoryId(),
                request.getMesReferencia(), request.getLimite());
    }

    @GetMapping("/user/{userId}")
    public List<BudgetSummaryResponse> listByUserAndMonth(@PathVariable Long userId,
                                                          @RequestParam YearMonth month) {
        return service.listByUserAndMonth(userId, month);
    }

    @PutMapping("/{id}/limit")
    public BudgetSummaryResponse updateLimit(@PathVariable Long id,
                                             @Valid @RequestBody BudgetLimitRequest request) {
        return service.updateLimit(id, request.getLimite());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
