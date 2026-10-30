package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    @Autowired
    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    public String login(String cpfInformado, String senha) {
        String cpf = cpfInformado.replaceAll("[^0-9]", "");

        loginAttemptService.checkNotLocked(cpf);

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(cpf, senha));
        } catch (AuthenticationException e) {
            boolean bloqueou = loginAttemptService.registerFailure(cpf);
            if (bloqueou) {
                loginAttemptService.notifyLock(cpf);
            }
            loginAttemptService.checkNotLocked(cpf);
            throw new BadCredentialsException("CPF ou senha inválidos");
        }

        loginAttemptService.registerSuccess(cpf);
        return jwtService.generateToken(cpf);
    }
}
