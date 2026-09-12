package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return repository.findAll();
    }

    public Optional<User> findById(Long id) {
        return repository.findById(id);
    }

    public User save(User user) {
        user.setCpf(user.getCpf().replaceAll("[^0-9]", ""));
        user.setSenhaHash(passwordEncoder.encode(user.getSenhaHash()));
        return repository.save(user);
    }

    public User update(Long id, User user) {
        User exist = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario não encontrado com id: " + id));
        exist.setName(user.getName());
        exist.setNumeroTelefone(user.getNumeroTelefone());
        exist.setEmail(user.getEmail());
        exist.setDataNascimento(user.getDataNascimento());
        exist.setBalance(user.getBalance());
        return repository.save(exist);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Optional<User> findByCpf(String cpf) {
        return repository.findByCpf(cpf);
    }

    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email);
    }
}