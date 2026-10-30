package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    Optional<User> findForShopUpdate(
            @org.springframework.data.repository.query.Param("id") Long id);

    Optional<User> findByCpf(String cpf);

    Optional<User> findByEmail(String email);

    Optional<User> findByNumeroTelefone(String numeroTelefone);
}
