package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.PixKeyAlreadyExistsException;
import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.PixKey;
import com.georgesbouanni.controle_gastos.model.PixKeyType;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.PixKeyRepository;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PixKeyService {

    private final PixKeyRepository repository;
    private final UserRepository userRepository;

    @Autowired
    public PixKeyService(PixKeyRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public PixKey create(Long userId, PixKeyType type, String keyValue) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + userId));

        // se for ? recebe uma chave pix aleatoria, se for : signifca que a variavel recebe o valor da keyValue que pode ser um cpf, email
        String finalKeyValue = (type == PixKeyType.ALEATORIA)
                ? UUID.randomUUID().toString()
                : keyValue;

        if (repository.existsByKeyValue(finalKeyValue)) {
            throw new PixKeyAlreadyExistsException("Essa chave pix já está em uso.");
        }

        PixKey pixKey = new PixKey(user, type, finalKeyValue);
        return repository.save(pixKey);
    }

    public List<PixKey> listByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    public PixKey findByKeyValue(String keyValue) {
        return repository.findByKeyValue(keyValue)
                .orElseThrow(() -> new ResourceNotFoundException("Chave Pix não encontrada: " + keyValue));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
