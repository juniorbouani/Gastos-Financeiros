package com.georgesbouanni.controle_gastos.controller;

import com.georgesbouanni.controle_gastos.dto.CategoryRequest;
import com.georgesbouanni.controle_gastos.model.Category;
import com.georgesbouanni.controle_gastos.security.CurrentUserService;
import com.georgesbouanni.controle_gastos.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService service;
    private final CurrentUserService currentUser;

    @Autowired
    public CategoryController(CategoryService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category create(@Valid @RequestBody CategoryRequest request) {
        return service.create(currentUser.getId(), request.getName(), request.getIcone());
    }

    @GetMapping("/user/{userId}")
    public List<Category> listByUser(@PathVariable Long userId) {
        currentUser.requireSameUser(userId);
        return service.listByUser(userId);
    }

    @GetMapping("/{id}")
    public Category findById(@PathVariable Long id) {
        return service.findByIdForUser(id, currentUser.getId());
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.update(id, currentUser.getId(), request.getName(), request.getIcone());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
