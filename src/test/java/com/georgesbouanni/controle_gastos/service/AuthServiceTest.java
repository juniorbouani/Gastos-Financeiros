package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.security.JwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)

public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService service;

    @Test
    void senhaErradaRegistraFalhaELancaCredencialInvalida() {
        when(authenticationManager.authenticate(Mockito.any()))
                .thenThrow(new BadCredentialsException("x"));

        Assertions.assertThrows(BadCredentialsException.class,
                () -> service.login("123.456.789-00", "errada"));

        Mockito.verify(loginAttemptService).registerFailure("12345678900");
        Mockito.verify(jwtService, Mockito.never()).generateToken(Mockito.anyString());
        Mockito.verify(loginAttemptService, Mockito.never()).notifyLock(Mockito.anyString());
    }

    @Test
    void loginCorretoGeraTokenEZeraTentativas() {
        when(jwtService.generateToken("12345678900")).thenReturn("token");

        String token = service.login("123.456.789-00", "certa");

        Assertions.assertEquals("token", token);
        Mockito.verify(loginAttemptService).registerSuccess("12345678900");
    }

    @Test
    void quintaFalhaNotificaOBloqueio() {
        when(authenticationManager.authenticate(Mockito.any())).thenThrow(new BadCredentialsException("x"));
        when(loginAttemptService.registerFailure("12345678900")).thenReturn(true);

        Assertions.assertThrows(BadCredentialsException.class,
                () -> service.login("123.456.789-00", "errada"));

        Mockito.verify(loginAttemptService).notifyLock("12345678900");
    }
}
