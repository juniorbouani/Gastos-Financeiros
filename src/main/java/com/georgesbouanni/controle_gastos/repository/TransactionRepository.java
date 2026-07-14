package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.model.Transaction;
import com.georgesbouanni.controle_gastos.model.TransactionType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends MongoRepository<Transaction, String> {

    List<Transaction> findByType(TransactionType type);

    List<Transaction> findByDateBetween(LocalDate start, LocalDate end);

    List<Transaction> findByCategory(String category);
}
