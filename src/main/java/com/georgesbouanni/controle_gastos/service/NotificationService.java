package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.Notification;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    @Autowired

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Notification create(User user, String mensagem) {
        return repository.save(new Notification(user, mensagem, LocalDateTime.now(), false));
    }

    public List<Notification> listByUser(Long userId) {
        return repository.findByUserIdOrderByDataCriacaoDesc(userId);
    }

    public long countUnread(Long userId) {
        return repository.countByUserIdAndLidaFalse(userId);
    }

    public Notification markAsRead(Long id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada com id: " + id));
        notification.setLida(true);
        return repository.save(notification);
    }

}
