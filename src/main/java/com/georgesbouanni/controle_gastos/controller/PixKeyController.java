package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.dto.PixKeyRequest;
import com.georgesbouanni.controle_gastos.model.PixKey;
import com.georgesbouanni.controle_gastos.repository.PixKeyRepository;
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

    @Autowired
    public PixKeyController(PixKeyService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PixKey create(@Valid @RequestBody PixKeyRequest request) {
        return service.create(request.getUserId(), request.getKeyType(), request.getKeyValue());
    }

    @GetMapping("/user/{userId}")
    public List<PixKey> listByUser(@PathVariable Long userId) {
        return service.listByUser(userId);
    }

    @GetMapping("/{keyValue}")
    public PixKey findKeyValue(@PathVariable String keyValue) {
        return service.findByKeyValue(keyValue);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
