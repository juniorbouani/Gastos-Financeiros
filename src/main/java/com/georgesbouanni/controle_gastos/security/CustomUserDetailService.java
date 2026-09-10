package com.georgesbouanni.controle_gastos.security;

import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String cpf) throws UsernameNotFoundException {
        User user = userRepository.findByCpf(cpf)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com cpf: " + cpf));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getCpf())
                .password(user.getSenhaHash())
                .authorities(Collections.emptyList())
                .build();
    }
}
