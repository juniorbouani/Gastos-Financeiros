package com.georgesbouanni.controle_gastos.repository;

import com.georgesbouanni.controle_gastos.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {

    List<User> findById(User user);

}
