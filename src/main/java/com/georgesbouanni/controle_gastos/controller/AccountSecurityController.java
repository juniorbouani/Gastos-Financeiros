package com.georgesbouanni.controle_gastos.controller;
import com.georgesbouanni.controle_gastos.dto.AccountCloseRequest;
import com.georgesbouanni.controle_gastos.dto.PasswordChangeRequest;
import com.georgesbouanni.controle_gastos.security.CurrentUserService;
import com.georgesbouanni.controle_gastos.service.AccountSecurityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/users/me")
public class AccountSecurityController {
    private final AccountSecurityService service;
    private final CurrentUserService current;
    public AccountSecurityController(AccountSecurityService service, CurrentUserService current) { this.service=service; this.current=current; }
    @PutMapping("/password")
    public ResponseEntity<Void> password(@Valid @RequestBody PasswordChangeRequest request) {
        service.changePassword(current.getId(),request); return ResponseEntity.noContent().build();
    }
    @PostMapping("/close")
    public ResponseEntity<Void> close(@Valid @RequestBody AccountCloseRequest request) {
        service.closeAccount(current.getId(),request); return ResponseEntity.noContent().build();
    }
}
