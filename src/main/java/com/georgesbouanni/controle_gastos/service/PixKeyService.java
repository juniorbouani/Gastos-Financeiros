package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.ForbiddenException;
import com.georgesbouanni.controle_gastos.exception.PixKeyAlreadyExistsException;
import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.PixKey;
import com.georgesbouanni.controle_gastos.model.PixKeyType;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.PixKeyRepository;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PixKeyService {

    private final PixKeyRepository repository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired
    public PixKeyService(PixKeyRepository repository, UserRepository userRepository, NotificationService notificationService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public PixKey create(Long userId, PixKeyType type, String keyValue) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + userId));

        String finalKeyValue;

        if (type == PixKeyType.ALEATORIA) {
            finalKeyValue = UUID.randomUUID().toString();
        } else if (type == PixKeyType.EMAIL) {
            finalKeyValue = keyValue.trim().toLowerCase();
        } else {
            finalKeyValue = keyValue.trim();
        }

        if (repository.existsByKeyValue(finalKeyValue)) {
            throw new PixKeyAlreadyExistsException("Essa chave PIX já está em uso.");
        }

        PixKey saved = repository.save(new PixKey(user, type, finalKeyValue));
        notificationService.create(user, "Chave PIX cadastrada (" + type + "): " + finalKeyValue + ".");
        return saved;
    }

    public List<PixKey> listByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    public PixKey findByKeyValue(String keyValue) {
        return repository.findByKeyValue(keyValue)
                .orElseThrow(() -> new ResourceNotFoundException("Chave Pix não encontrada: " + keyValue));
    }

    @Transactional
    public void delete(Long id, Long userId) {
        PixKey pixKey = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chave PIX não encontrada com id: " + id));
        if (!pixKey.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Esta chave PIX não pertence a você.");
        }
        repository.delete(pixKey);
        notificationService.create(pixKey.getUser(),
                "Chave PIX removida (" + pixKey.getKeyType() + "): " + pixKey.getKeyValue() + ".");
    }
}
