package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.*;
import com.georgesbouanni.controle_gastos.model.*;
import com.georgesbouanni.controle_gastos.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock CategoryRepository repository;
    @Mock UserRepository users;
    @InjectMocks CategoryService service;

    User owner() {
        User u = new User();
        u.setId(7L);
        return u;
    }

    @Test
    void duplicateCategoryProducesBusinessError() {
        User u = owner();
        when(users.findById(7L)).thenReturn(Optional.of(u));
        when(repository.findByUserIdAndNameIgnoreCase(7L, "Casa"))
                .thenReturn(Optional.of(new Category(u, "Casa", "")));
        assertThrows(BusinessRuleException.class, () -> service.create(7L, " Casa ", ""));
        verify(repository, never()).save(any());
    }

    @Test
    void editCannotRenameToAnotherExistingCategory() {
        User u = owner();
        Category original = new Category(u, "Casa", "");
        original.setId(1L);
        Category duplicate = new Category(u, "Mercado", "");
        duplicate.setId(2L);
        when(repository.findById(1L)).thenReturn(Optional.of(original));
        when(repository.findByUserIdAndNameIgnoreCase(7L, "Mercado"))
                .thenReturn(Optional.of(duplicate));
        assertThrows(BusinessRuleException.class, () -> service.update(1L, 7L, "Mercado", ""));
        assertEquals("Casa", original.getName());
        verify(repository, never()).save(any());
    }

    @Test
    void editCannotAccessAnotherUsersCategory() {
        Category c = new Category(owner(), "Casa", "");
        c.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        assertThrows(ForbiddenException.class, () -> service.update(1L, 8L, "Nome", ""));
        verify(repository, never()).save(any());
    }
}
