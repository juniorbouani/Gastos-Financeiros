package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.model.Transaction;
import com.georgesbouanni.controle_gastos.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByType(TransactionType type);

    List<Transaction> findByDateBetween(LocalDate start, LocalDate end);

    List<Transaction> findBySenderId(Long senderId);

    List<Transaction> findByReceiverId(Long receiverId);

    List<Transaction> findBySenderIdOrReceiverId(Long senderId, Long receiverId);
}