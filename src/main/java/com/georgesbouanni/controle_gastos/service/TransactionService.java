package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.Transaction;
import com.georgesbouanni.controle_gastos.model.TransactionType;
import com.georgesbouanni.controle_gastos.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository repository;

    @Autowired
    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public List<Transaction> listAll() {
        return repository.findAll();
    }

    public Optional<Transaction> findById(String id) {
        return repository.findById(id);
    }

    public Transaction save(Transaction transaction) {
        return repository.save(transaction);
    }

    public Transaction update(String id, Transaction updatedData) {
        Transaction existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada com id: " + id));
        existing.setDescription(updatedData.getDescription());
        existing.setValue(updatedData.getValue());
        existing.setDate(updatedData.getDate());
        existing.setType(updatedData.getType());
        existing.setCategory(updatedData.getCategory());

        return repository.save(existing);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    public List<Transaction> listByType(TransactionType type) {
        return repository.findByType(type);
    }

    public List<Transaction> listByPeriod(LocalDate start, LocalDate end) {
        return repository.findByDateBetween(start, end);
    }
}
