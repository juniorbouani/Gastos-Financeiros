package com.georgesbouanni.controle_gastos.service;

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

        if (repository.findByUserIdAndNameIgnoreCase(userId, name).isPresent()) {
            throw new IllegalArgumentException("Você já possui uma categoria com esse nome");
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

    public Category update(Long id, String name, String icone) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + id));
        category.setName(name);
        category.setIcone(icone);
        return repository.save(category);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

}