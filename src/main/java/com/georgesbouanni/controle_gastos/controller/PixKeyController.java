package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.dto.PixKeyRequest;
import com.georgesbouanni.controle_gastos.model.PixKey;
import com.georgesbouanni.controle_gastos.repository.PixKeyRepository;
import com.georgesbouanni.controle_gastos.security.CurrentUserService;
import com.georgesbouanni.controle_gastos.service.PixKeyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pix-keys")
public class PixKeyController {

    private final PixKeyService service;
    private final CurrentUserService currentUser;

    @Autowired
    public PixKeyController(PixKeyService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PixKey create(@Valid @RequestBody PixKeyRequest request) {
        return service.create(currentUser.getId(), request.getKeyType(), request.getKeyValue());
    }

    @GetMapping("/user/{userId}")
    public List<PixKey> listByUser(@PathVariable Long userId) {
        currentUser.requireSameUser(userId);
        return service.listByUser(userId);
    }

    @GetMapping("/{keyValue}")
    public PixKey findKeyValue(@PathVariable String keyValue) {
        return service.findByKeyValue(keyValue);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
