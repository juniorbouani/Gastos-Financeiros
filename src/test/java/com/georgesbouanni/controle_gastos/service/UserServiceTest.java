package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.DataAlreadyExistsException;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock private UserRepository repository;

    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService service;

    private User novoUsuario() {
        User u = new User();
        u.setName("Teste");
        u.setCpf("123.456.789-00");
        u.setNumeroTelefone("(11) 99999-9999");
        u.setEmail("Teste@Email.com");
        u.setSenhaHash("senha");
        u.setBalance(BigDecimal.ZERO);
        u.setDataNascimento(LocalDate.of(2000, 1, 1));
        return u;
    }

    @Test
    void rejeitarCpfDuplicado() {
        when(repository.findByCpf("12345678900")).thenReturn(Optional.of(new User()));

        Assertions.assertThrows(
                DataAlreadyExistsException.class, () -> service.save(novoUsuario()));
        Mockito.verify(repository, Mockito.never()).save(Mockito.any(User.class));
    }

    @Test
    void normalizarDadosAntesDeSalvar() {
        when(passwordEncoder.encode("senha")).thenReturn("hash");
        when(repository.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User salvo = service.save(novoUsuario());

        Assertions.assertEquals("12345678900", salvo.getCpf());
        Assertions.assertEquals("11999999999", salvo.getNumeroTelefone());
        Assertions.assertEquals("teste@email.com", salvo.getEmail());
        Assertions.assertEquals("hash", salvo.getSenhaHash());
    }
}
