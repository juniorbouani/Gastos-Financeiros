package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.dto.UserResponse;
import com.georgesbouanni.controle_gastos.dto.UserUpdateRequest;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.security.CurrentUserService;
import com.georgesbouanni.controle_gastos.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;
    private final CurrentUserService currentUser;

    @Autowired
    public UserController(UserService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody User user) {
        return UserResponse.from(service.save(user));
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(currentUser.get());
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        currentUser.requireSameUser(id);
        return UserResponse.from(service.update(id, request));
    }
}
