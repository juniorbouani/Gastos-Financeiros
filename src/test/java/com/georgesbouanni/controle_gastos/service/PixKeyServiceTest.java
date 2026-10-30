package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.ForbiddenException;
import com.georgesbouanni.controle_gastos.model.PixKey;
import com.georgesbouanni.controle_gastos.model.PixKeyType;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.PixKeyRepository;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PixKeyServiceTest {

    @Mock private PixKeyRepository repository;

    @Mock private UserRepository userRepository;

    @Mock private NotificationService notificationService;

    @InjectMocks private PixKeyService service;

    private User usuario() {
        User u = new User();
        u.setId(1L);
        return u;
    }

    @Test
    void cadastrarChaveNotificaComChaveNormalizada() {
        User user = usuario();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.save(Mockito.any(PixKey.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(1L, PixKeyType.EMAIL, "Ana@Email.com");

        Mockito.verify(notificationService)
                .create(Mockito.eq(user), Mockito.contains("ana@email.com"));
    }

    @Test
    void removerChaveNotificaODono() {
        User user = usuario();
        PixKey key = new PixKey(user, PixKeyType.EMAIL, "ana@email.com");
        when(repository.findById(5L)).thenReturn(Optional.of(key));

        service.delete(5L, 1L);

        Mockito.verify(repository).delete(key);
        Mockito.verify(notificationService).create(Mockito.eq(user), Mockito.contains("removida"));
    }

    @Test
    void negarRemocaoDeChaveDeOutroUsuario() {
        PixKey key = new PixKey(usuario(), PixKeyType.EMAIL, "ana@email.com");
        when(repository.findById(5L)).thenReturn(Optional.of(key));

        Assertions.assertThrows(ForbiddenException.class, () -> service.delete(5L, 2L));
        Mockito.verifyNoInteractions(notificationService);
    }
}
