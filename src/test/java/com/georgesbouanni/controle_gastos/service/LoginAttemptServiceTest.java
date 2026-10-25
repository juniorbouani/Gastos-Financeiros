package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.AccountLockedException;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginAttemptServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LoginAttemptService service;

    private User usuarioComTentativas(int tentativas) {
        User u = new User();
        u.setCpf("12345678900");
        u.setFailedLoginAttempts(tentativas);
        return u;
    }

    @Test
    void quintaFalhaBloqueiaEZeraOContador() {
        User user = usuarioComTentativas(4);
        when(userRepository.findByCpf("12345678900")).thenReturn(Optional.of(user));

        service.registerFailure("12345678900");

        Assertions.assertNotNull(user.getLockedUntil());
        Assertions.assertTrue(user.getLockedUntil().isAfter(LocalDateTime.now()));
        Assertions.assertEquals(0, user.getFailedLoginAttempts());
    }

    @Test
    void falhaAbaixoDoLimiteSoIncrementa() {
        User user = usuarioComTentativas(2);
        when(userRepository.findByCpf("12345678900")).thenReturn(Optional.of(user));

        service.registerFailure("12345678900");

        Assertions.assertEquals(3, user.getFailedLoginAttempts());
        Assertions.assertNull(user.getLockedUntil());
    }

    @Test
    void sucessoZeraContadorEBloqueio() {
        User user = usuarioComTentativas(3);
        when(userRepository.findByCpf("12345678900")).thenReturn(Optional.of(user));

        service.registerSuccess("12345678900");

        Assertions.assertEquals(0, user.getFailedLoginAttempts());
        Assertions.assertNull(user.getLockedUntil());
    }

    @Test
    void recusarLoginComContaBloqueada() {
        User user = usuarioComTentativas(0);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByCpf("12345678900")).thenReturn(Optional.of(user));

        Assertions.assertThrows(AccountLockedException.class, () -> service.checkNotLocked("12345678900"));
    }

    @Test
    void liberarLoginQuandoOBloqueioJaExpirou() {
        User user = usuarioComTentativas(0);
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByCpf("12345678900")).thenReturn(Optional.of(user));

        Assertions.assertDoesNotThrow(() -> service.checkNotLocked("12345678900"));
    }
}
