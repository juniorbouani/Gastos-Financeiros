package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.ForbiddenException;
import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.Notification;
import com.georgesbouanni.controle_gastos.model.Transaction;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    @Autowired
    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Notification create(User user, String mensagem) {
        return repository.save(new Notification(user, mensagem));
    }

    public void notifyTransfer(Transaction t) {
        String valor = MoneyFormatter.format(t.getValue());
        String tipo = t.getType().name();
        User sender = t.getSender();
        User receiver = t.getReceiver();

        if (receiver != null) {
            create(
                    sender,
                    "Você enviou " + valor + " para " + receiver.getName() + " (" + tipo + ").");
            create(
                    receiver,
                    "Você recebeu " + valor + " de " + sender.getName() + " (" + tipo + ").");
        } else {
            create(sender, "Você realizou um pagamento de " + valor + " (" + tipo + ").");
        }
    }

    public void notifyReversal(Transaction reversal) {
        String valor = MoneyFormatter.format(reversal.getValue());
        User devolveu = reversal.getSender();
        User recebeu = reversal.getReceiver();

        create(
                recebeu,
                "Estorno recebido: " + valor + " devolvido por " + devolveu.getName() + ".");
        create(devolveu, "Você devolveu " + valor + " para " + recebeu.getName() + " (estorno).");
    }

    public Page<Notification> list(Long userId, boolean onlyUnread, Pageable pageable) {
        return onlyUnread
                ? repository.findByUserIdAndLidaFalse(userId, pageable)
                : repository.findByUserId(userId, pageable);
    }

    public long countUnread(Long userId) {
        return repository.countByUserIdAndLidaFalse(userId);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return repository.markAllAsRead(userId);
    }

    public Notification markAsRead(Long id, Long userId) {
        Notification notification =
                repository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Notificação não encontrada com id: " + id));
        if (!notification.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Esta notificação não pertence a você.");
        }
        notification.setLida(true);
        return repository.save(notification);
    }
}
