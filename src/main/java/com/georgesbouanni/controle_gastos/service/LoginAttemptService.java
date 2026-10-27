package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.AccountLockedException;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final  UserRepository userRepository;

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final NotificationService notificationService;


@Autowired
    public LoginAttemptService(UserRepository userRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
    public void registerSuccess(String cpf) {
        userRepository.findByCpf(cpf).ifPresent(user -> {
            if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        });
    }

    @Transactional
    public boolean registerFailure(String cpf) {
        return userRepository.findByCpf(cpf).map(user -> {
            int tentativas = user.getFailedLoginAttempts() + 1;
            boolean bloqueou = tentativas >= MAX_ATTEMPTS;

            if (bloqueou) {
                user.setLockedUntil(LocalDateTime.now().plus(LOCK_DURATION));
                user.setFailedLoginAttempts(0);
            } else {
                user.setFailedLoginAttempts(tentativas);
            }
            userRepository.save(user);
            return bloqueou;
        }).orElse(false);
    }

    public void notifyLock(String cpf) {
        userRepository.findByCpf(cpf).ifPresent(user -> {
            try {
                notificationService.create(user,
                        "Sua conta foi bloqueada por " + LOCK_DURATION.toMinutes() + " minutos após "
                                + MAX_ATTEMPTS + " tentativas de login incorretas. "
                                + "Se não foi você, fique atento: alguém pode estar tentando acessar sua conta.");
            } catch (RuntimeException e) {
                log.warn("Falha ao registrar a notificação de bloqueio do usuário {}", user.getId(), e);
            }
        });
    }

}