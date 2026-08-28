package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByCPF(String cpf);

    Optional<User> findByEmail(String emaiil);
}
