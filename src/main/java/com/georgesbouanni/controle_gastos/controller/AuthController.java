package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.dto.LoginRequest;
import com.georgesbouanni.controle_gastos.dto.LoginResponse;
import com.georgesbouanni.controle_gastos.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCpf(), request.getSenha())
            );
        } catch (Exception e) {
            throw new BadCredentialsException("CPF ou senha inválidos");
        }

        String token = jwtService.generateToken(request.getCpf());
        return new LoginResponse(token);
    }
}
