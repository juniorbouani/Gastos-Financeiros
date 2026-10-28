package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.dto.CategorySpent;
import com.georgesbouanni.controle_gastos.model.Transaction;
import com.georgesbouanni.controle_gastos.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
          AND NOT EXISTS (SELECT r.id FROM Transaction r WHERE r.reversalOf.id = t.id)
        """)
    BigDecimal sumSpent(@Param("userId") Long userId,
                        @Param("categoryId") Long categoryId,
                        @Param("start") LocalDate start,
                        @Param("end") LocalDate end);

    Page<Transaction> findBySenderIdOrReceiverId(Long senderId, Long receiverId, Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t
        WHERE (t.sender.id = :userId OR t.receiver.id = :userId)
          AND t.type = :type
        """)
    List<Transaction> findByUserAndType(@Param("userId") Long userId, @Param("type") TransactionType type);

    @Query("""
        SELECT t FROM Transaction t
        WHERE (t.sender.id = :userId OR t.receiver.id = :userId)
          AND t.date BETWEEN :start AND :end
        """)
    List<Transaction> findByUserAndPeriod(@Param("userId") Long userId,
                                          @Param("start") LocalDate start,
                                          @Param("end") LocalDate end);

    boolean existsByReversalOfId(Long reversalOfId);

    @Query("""
        SELECT new com.georgesbouanni.controle_gastos.dto.CategorySpent(c.id, c.name, SUM(t.value), COUNT(t))
        FROM Transaction t LEFT JOIN t.category c
        WHERE t.sender.id = :userId
          AND t.date BETWEEN :start AND :end
          AND t.status = com.georgesbouanni.controle_gastos.model.TransactionStatus.COMPLETED
          AND t.type <> com.georgesbouanni.controle_gastos.model.TransactionType.ESTORNO
          AND NOT EXISTS (SELECT r.id FROM Transaction r WHERE r.reversalOf.id = t.id)
        GROUP BY c.id, c.name
        """)
    List<CategorySpent> spentByCategory(@Param("userId") Long userId,
                                        @Param("start") LocalDate start,
                                        @Param("end") LocalDate end);

    @Query("""
        SELECT COALESCE(SUM(t.value), 0)
        FROM Transaction t
        WHERE t.receiver.id = :userId
          AND t.date BETWEEN :start AND :end
          AND t.status = com.georgesbouanni.controle_gastos.model.TransactionStatus.COMPLETED
          AND t.type <> com.georgesbouanni.controle_gastos.model.TransactionType.ESTORNO
          AND NOT EXISTS (SELECT r.id FROM Transaction r WHERE r.reversalOf.id = t.id)
        """)
    BigDecimal sumReceived(@Param("userId") Long userId,
                           @Param("start") LocalDate start,
                           @Param("end") LocalDate end);
}

