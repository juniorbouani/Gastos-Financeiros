package com.georgesbouanni.controle_gastos.security;

import com.georgesbouanni.controle_gastos.exception.ForbiddenException;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    @Autowired
    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User get() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            throw new ForbiddenException("Você precisa estar autenticado");
        }

        return userRepository.findByCpf(authentication.getName())
                .orElseThrow(() -> new ForbiddenException("Usuário do token não encontrado."));
    }

    public Long getId(){
        return get().getId();
    }
}
