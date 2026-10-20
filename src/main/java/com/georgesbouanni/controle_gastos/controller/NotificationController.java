package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.model.Notification;
import com.georgesbouanni.controle_gastos.security.CurrentUserService;
import com.georgesbouanni.controle_gastos.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;
    private final CurrentUserService currentUser;


    @Autowired
    public NotificationController(NotificationService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/user/{userId}")
    public List<Notification> listByUser(@PathVariable Long userId) {
        currentUser.requireSameUser(userId);
        return service.listByUser(userId);
    }

    @GetMapping("/user/{userId}/unread-count")
    public long countUnread(@PathVariable Long userId) {
        currentUser.requireSameUser(userId);
        return service.countUnread(userId);
    }

    @PutMapping("/{id}/read")
    public Notification markAsRead(@PathVariable Long id) {
        return service.markAsRead(id, currentUser.getId());
    }
}
