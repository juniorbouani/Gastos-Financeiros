package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.dto.TransactionRequest;
import com.georgesbouanni.controle_gastos.dto.TransactionResponse;
import com.georgesbouanni.controle_gastos.model.Transaction;
import com.georgesbouanni.controle_gastos.model.TransactionType;
import com.georgesbouanni.controle_gastos.security.CurrentUserService;
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
    private final CurrentUserService currentUser;

    @Autowired
    public TransactionController(TransactionService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public Page<TransactionResponse> list(Pageable pageable) {
        return service.listByUserPaginated(currentUser.getId(), pageable).map(TransactionResponse::from);
    }

    @GetMapping("/{id}")
    public TransactionResponse findById(@PathVariable Long id) {
        return TransactionResponse.from(service.findByIdForUser(id, currentUser.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) {
        return TransactionResponse.from(service.save(request, currentUser.getId()));
    }

    @GetMapping("/type/{type}")
    public List<TransactionResponse> listByType(@PathVariable TransactionType type) {
        return service.listByUserAndType(currentUser.getId(), type).stream()
                .map(TransactionResponse::from).toList();
    }

    @GetMapping("/period")
    public List<TransactionResponse> listByPeriod(@RequestParam LocalDate start, @RequestParam LocalDate end) {
        return service.listByUserAndPeriod(currentUser.getId(), start, end).stream()
                .map(TransactionResponse::from).toList();
    }

    @GetMapping("/user/{userId}")
    public List<TransactionResponse> listByUser(@PathVariable Long userId) {
        currentUser.requireSameUser(userId);
        return service.listByUser(userId).stream().map(TransactionResponse::from).toList();
    }

    @PostMapping("/{id}/reverse")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse reverse(@PathVariable Long id) {
        return TransactionResponse.from(service.reverse(id, currentUser.getId()));
    }
}