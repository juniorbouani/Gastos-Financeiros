package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.dto.LoginRequest;
import com.georgesbouanni.controle_gastos.dto.LoginResponse;
import com.georgesbouanni.controle_gastos.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return new LoginResponse(authService.login(request.getCpf(), request.getSenha()));
    }
}
