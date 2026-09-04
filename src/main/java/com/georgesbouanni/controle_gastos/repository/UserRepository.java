package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByCpf(String cpf);

    Optional<User> findByEmail(String email);
}