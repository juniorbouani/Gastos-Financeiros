package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.dto.TransactionRequest;
import com.georgesbouanni.controle_gastos.model.Transaction;
import com.georgesbouanni.controle_gastos.model.TransactionType;
import com.georgesbouanni.controle_gastos.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService service;

    @Autowired
    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping
    public Page<Transaction> list(Pageable pageable) {
        return service.listAllPaginated(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> findById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction create(@Valid @RequestBody TransactionRequest request) {
        return service.save(request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> update(@PathVariable Long id, @Valid @RequestBody Transaction transaction) {
        Transaction updated = service.update(id, transaction);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/type/{type}")
    public List<Transaction> listByType(@PathVariable TransactionType type) {
        return service.listByType(type);
    }

    @GetMapping("/period")
    public List<Transaction> listByPeriod(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end) {
        return service.listByPeriod(start, end);
    }

    @GetMapping("/user/{userId}")
    public List<Transaction> listByUser(@PathVariable Long userId) {
        return service.listByUser(userId);
    }
}