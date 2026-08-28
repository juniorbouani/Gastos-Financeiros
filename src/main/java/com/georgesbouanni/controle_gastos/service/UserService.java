package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repository;

    @Autowired
    public UserService(UserRepository repository) { this.repository = repository; }

    public List<User> findAll() { return repository.findAll(); }

    public Optional<User> findById(String id) { return repository.findById(id); }

    public User save(User user) { return repository.save(user); }

    public User update(String id,User user) {
        User exist = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario não encontrado com id: " + id));
        exist.setName(user.getName());
        exist.setNumeroTelefone(user.getNumeroTelefone());
        exist.setEmail(user.getEmail());
        exist.setDataNascimento(user.getDataNascimento());
        exist.setBalance(user.getBalance());

        return repository.save(exist);
    }

    public void delete(String id) { repository.deleteById(id); }
}