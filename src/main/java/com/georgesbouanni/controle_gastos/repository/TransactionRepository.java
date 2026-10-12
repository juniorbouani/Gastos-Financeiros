package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.model.Transaction;
import com.georgesbouanni.controle_gastos.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByType(TransactionType type);

    List<Transaction> findByDateBetween(LocalDate start, LocalDate end);

    List<Transaction> findBySenderId(Long senderId);

    List<Transaction> findByReceiverId(Long receiverId);

    List<Transaction> findBySenderIdOrReceiverId(Long senderId, Long receiverId);

    @Query("""
            SELECT COALESCE(SUM(t.value), 0)
            FROM Transaction t
            WHERE t.sender.id = :userId
                AND t.category.id = :categoryId
                AND t.date BETWEEN :start AND :end
                AND t.status = com.georgesbouanni.controle_gastos.model.TransactionStatus.COMPLETED
     """)
    BigDecimal sumSpent(@Param("userId") Long userId,
                        @Param("categoryId") Long categoryId,
                        @Param("start") LocalDate start,
                        @Param("end") LocalDate end);
}