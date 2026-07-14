package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.model.Transaction;
import com.georgesbouanni.controle_gastos.model.TransactionType;
import com.georgesbouanni.controle_gastos.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> findById(@PathVariable String id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction create(@Valid @RequestBody Transaction transaction) {
        return service.save(transaction);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> update(@PathVariable String id, @Valid @RequestBody Transaction transaction) {
        Transaction updated = service.update(id, transaction);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id){
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
}
