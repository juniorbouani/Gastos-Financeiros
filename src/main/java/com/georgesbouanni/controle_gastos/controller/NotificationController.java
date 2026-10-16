package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.model.Notification;
import com.georgesbouanni.controle_gastos.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    @Autowired
    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/user/{userId}")
    public List<Notification> listByUser(@PathVariable Long userId) {
        return service.listByUser(userId);
    }

    @GetMapping("/user/{userId}/unread-count")
    public long countUnread(@PathVariable Long userId) {
        return service.countUnread(userId);
    }

    @PutMapping("/{id}/read")
    public Notification markAsRead(@PathVariable Long id) {
        return service.markAsRead(id);
    }
}
