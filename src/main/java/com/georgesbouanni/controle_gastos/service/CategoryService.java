package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.ForbiddenException;
import com.georgesbouanni.controle_gastos.exception.BusinessRuleException;
import com.georgesbouanni.controle_gastos.exception.ResourceNotFoundException;
import com.georgesbouanni.controle_gastos.model.Category;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.repository.CategoryRepository;
import com.georgesbouanni.controle_gastos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository repository;
    private final UserRepository userRepository;

    @Autowired
    public CategoryService(CategoryRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public Category create(Long userId, String name, String icone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com a id: " + userId));

        name = name.trim();
        if (repository.findByUserIdAndNameIgnoreCase(userId, name).isPresent()) {
            throw new BusinessRuleException("Você já possui uma categoria com esse nome.");
        }

        Category category = new Category(user, name, icone);
        return repository.save(category);
    }

    public List<Category> listByUser(Long userId) {
        return repository.findByUserId(userId);
    }

    public Optional<Category> findById(Long id) {
        return repository.findById(id);
    }

    public Category findByIdForUser(Long id, Long userId) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + id));
        if (!category.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Esta categoria não pertence a você.");
        }
        return category;
    }

    public Category update(Long id, Long userId, String name, String icone) {
        Category category = findByIdForUser(id, userId);
        String normalized = name.trim();
        var duplicate = repository.findByUserIdAndNameIgnoreCase(userId, normalized);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id))
            throw new BusinessRuleException("Você já possui uma categoria com esse nome.");
        category.setName(normalized);
        category.setIcone(icone);
        return repository.save(category);
    }

    public void delete(Long id, Long userId) {
        repository.delete(findByIdForUser(id, userId));
    }

}