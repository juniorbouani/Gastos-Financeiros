package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable; // ✅ CORRETO

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByDataCriacaoDesc(Long userId);

    long countByUserIdAndLidaFalse(Long userId);

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndLidaFalse(Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.lida = true WHERE n.user.id = :userId AND n.lida = false")
    int markAllAsRead(@Param("userId") Long userId);

}