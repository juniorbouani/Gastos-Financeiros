package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.AccountLockedException;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPT = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final  UserRepository userRepository;

    @Autowired
    public LoginAttemptService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void checkNotLocked(String cpf) {
        userRepository.findByCpf(cpf).ifPresent(user -> {
            LocalDateTime lockedUntil = user.getLockedUntil();
            LocalDateTime agora = LocalDateTime.now();

            if (lockedUntil != null && lockedUntil.isAfter(agora)) {
                long segundos = Duration.between(agora, lockedUntil).getSeconds();
                long minutos = Math.max(1, (segundos + 59) / 60);
                throw new AccountLockedException(
                        "Muitas tentativas de login. Tente novamente em " + minutos + " minuto(s)."
                );
            }
        });
    }

    @Transactional
    public void registerFailure(String cpf) {
        userRepository.findByCpf(cpf).ifPresent(user -> {
            int tentativas = user.getFailedLoginAttempts() + 1;

            if (tentativas >= MAX_ATTEMPT) {
                user.setLockedUntil(LocalDateTime.now().plus(LOCK_DURATION));
                user.setFailedLoginAttempts(0);
            } else {
                user.setFailedLoginAttempts(tentativas);
            }
            userRepository.save(user);
        });

    }

    @Transactional
    public void registerSuccess(String cpf) {
            userRepository.findByCpf(cpf).ifPresent(user -> {
                if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
                    user.setFailedLoginAttempts(0);
                    user.setLockedUntil(null);
                    userRepository.save(user);
                }
            });
    }
}