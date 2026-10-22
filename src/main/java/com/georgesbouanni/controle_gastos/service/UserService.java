package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.dto.UserUpdateRequest;
import com.georgesbouanni.controle_gastos.exception.BusinessRuleException;
import com.georgesbouanni.controle_gastos.exception.DataAlreadyExistsException;
import com.georgesbouanni.controle_gastos.exception.EmailAlreadyExistsException;
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
        String email = user.getEmail().trim().toLowerCase();
        String cpf = user.getCpf().replaceAll("[^0-9]", "");
        String telefone = user.getNumeroTelefone().replaceAll("[^0-9]", "");

        if (cpf.length() != 11) {
            throw new BusinessRuleException("O CPF deve conter exatamente 11 dígitos.");
        }
        if (repository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException("Esse e-mail já está cadastrado.");
        }
        if (repository.findByCpf(cpf).isPresent()) {
            throw new DataAlreadyExistsException("Esse CPF já está cadastrado.");
        }
        if (repository.findByNumeroTelefone(telefone).isPresent()) {
            throw new DataAlreadyExistsException("Esse telefone já está cadastrado.");
        }

        user.setEmail(email);
        user.setCpf(cpf);
        user.setNumeroTelefone(telefone);
        user.setSenhaHash(passwordEncoder.encode(user.getSenhaHash()));
        return repository.save(user);
    }

    public User update(Long id, UserUpdateRequest request) {
        User exist = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario não encontrado com id: " + id));

        String email = request.getEmail().trim().toLowerCase();
        String telefone = request.getNumeroTelefone().replaceAll("[^0-9]", "");

        repository.findByEmail(email)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> { throw new EmailAlreadyExistsException("Esse e-mail já está cadastrado."); });
        repository.findByNumeroTelefone(telefone)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> { throw new DataAlreadyExistsException("Esse telefone já está cadastrado."); });

        exist.setName(request.getName());
        exist.setNumeroTelefone(telefone);
        exist.setEmail(email);
        exist.setDataNascimento(request.getDataNascimento());

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