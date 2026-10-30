package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.model.Notification;
import com.georgesbouanni.controle_gastos.security.CurrentUserService;
import com.georgesbouanni.controle_gastos.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping
    public Page<Notification> list(
            @RequestParam(defaultValue = "false") boolean onlyUnread,
            @PageableDefault(size = 20, sort = "dataCriacao", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return service.list(currentUser.getId(), onlyUnread, pageable);
    }

    @GetMapping("/unread-count")
    public long countUnread() {
        return service.countUnread(currentUser.getId());
    }

    @PutMapping("/read-all")
    public Map<String, Integer> markAllAsRead() {
        return Map.of("marcadas", service.markAllAsRead(currentUser.getId()));
    }

    @PutMapping("/{id}/read")
    public Notification markAsRead(@PathVariable Long id) {
        return service.markAsRead(id, currentUser.getId());
    }
}
