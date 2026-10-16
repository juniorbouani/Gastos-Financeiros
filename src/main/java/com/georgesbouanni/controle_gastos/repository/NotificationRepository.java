package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByDataCriacaoDesc(Long userId);

    long countByUserIdAndLidaFalse(Long userId);

}