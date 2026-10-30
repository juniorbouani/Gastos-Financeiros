package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.dto.AccountCloseRequest;
import com.georgesbouanni.controle_gastos.dto.PasswordChangeRequest;
import com.georgesbouanni.controle_gastos.exception.*;
import com.georgesbouanni.controle_gastos.model.*;
import com.georgesbouanni.controle_gastos.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;

@Service
public class AccountSecurityService {
    private final UserRepository users;
    private final CreditCardRepository cards;
    private final InvoiceRepository invoices;
    private final PixKeyRepository keys;
    private final PasswordEncoder encoder;
    private final LoginAttemptService attempts;
    private final NotificationService notifications;

    public AccountSecurityService(UserRepository users, CreditCardRepository cards, InvoiceRepository invoices,
            PixKeyRepository keys, PasswordEncoder encoder, LoginAttemptService attempts, NotificationService notifications) {
        this.users=users; this.cards=cards; this.invoices=invoices; this.keys=keys;
        this.encoder=encoder; this.attempts=attempts; this.notifications=notifications;
    }
    private User authenticate(Long id, String password) {
        User user=users.findById(id).filter(u -> !u.isAccountClosed())
                .orElseThrow(() -> new ForbiddenException("Conta indisponível."));
        attempts.checkNotLocked(user.getCpf());
        boolean matches = password != null && password.getBytes(StandardCharsets.UTF_8).length <= 72
                && encoder.matches(password, user.getSenhaHash());
        if (!matches) {
            boolean locked=attempts.registerFailure(user.getCpf());
            if (locked) attempts.notifyLock(user.getCpf());
            attempts.checkNotLocked(user.getCpf());
            throw new BusinessRuleException("Senha atual incorreta.");
        }
        attempts.registerSuccess(user.getCpf());
        return user;
    }
    @Transactional(noRollbackFor = {BusinessRuleException.class, AccountLockedException.class})
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user=authenticate(userId,request.senhaAtual());
        String password=request.novaSenha();
        if (password==null || password.isBlank() || password.length()<8 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new BusinessRuleException("A nova senha deve ter pelo menos 8 caracteres e no máximo 72 bytes em UTF-8.");
        if (encoder.matches(password,user.getSenhaHash()))
            throw new BusinessRuleException("Escolha uma senha diferente da atual.");
        user.setSenhaHash(encoder.encode(password));
        user.setCredentialVersion(Math.addExact(user.getCredentialVersion(),1));
        users.save(user);
        notifications.create(user,"Sua senha foi alterada. Os acessos anteriores foram encerrados.");
    }
    @Transactional(noRollbackFor = {BusinessRuleException.class, AccountLockedException.class})
    public void closeAccount(Long userId, AccountCloseRequest request) {
        User user=authenticate(userId,request.senhaAtual());
        if (!"ENCERRAR".equals(request.confirmacao()))
            throw new BusinessRuleException("Digite ENCERRAR para confirmar.");
        if (user.getBalance()==null || user.getBalance().signum()!=0)
            throw new BusinessRuleException("O saldo deve estar zerado para encerrar a conta.");
        var ownedCards=cards.findByUserId(userId);
        for (var card:ownedCards) {
            boolean debt=invoices.findByCreditCardId(card.getId()).stream()
                    .anyMatch(i -> i.getValorTotal().compareTo(i.getValorPago())>0);
            if (debt) throw new BusinessRuleException("Quite todas as faturas, inclusive futuras, antes de encerrar a conta.");
        }
        for (var card:ownedCards) card.setStatus(CreditCardStatus.CANCELADO);
        cards.saveAll(ownedCards);
        keys.deleteAll(keys.findByUserId(userId));
        user.setAccountClosed(true);
        user.setCredentialVersion(Math.addExact(user.getCredentialVersion(),1));
        users.save(user);
    }
}
