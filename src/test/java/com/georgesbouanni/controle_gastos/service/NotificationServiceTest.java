package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.exception.ForbiddenException;
import com.georgesbouanni.controle_gastos.model.*;
import com.georgesbouanni.controle_gastos.repository.NotificationRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock private NotificationRepository repository;

    @InjectMocks private NotificationService service;

    @Test
    void criarNotificacaoPreencheUsuarioEMensagem() {
        User user = new User();
        user.setId(1L);

        service.create(user, "Orçamento atingido");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        Mockito.verify(repository).save(captor.capture());

        Assertions.assertEquals(user, captor.getValue().getUser());
        Assertions.assertEquals("Orçamento atingido", captor.getValue().getMensagem());
        Assertions.assertFalse(captor.getValue().isLida());
    }

    private Transaction transacao(User sender, User receiver) {
        return new Transaction(
                "Pix",
                new BigDecimal("150.00"),
                LocalDate.of(2026, 10, 25),
                TransactionType.PIX,
                TransactionStatus.COMPLETED,
                sender,
                receiver,
                null,
                null);
    }

    private User usuario(Long id, String nome) {
        User u = new User();
        u.setId(id);
        u.setName(nome);
        return u;
    }

    @Test
    void transferenciaNotificaQuemEnviouEQuemRecebeu() {
        User georges = usuario(1L, "Georges");
        User rafael = usuario(2L, "Rafael");

        service.notifyTransfer(transacao(georges, rafael));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        Mockito.verify(repository, Mockito.times(2)).save(captor.capture());
        List<Notification> salvas = captor.getAllValues();

        Assertions.assertEquals(georges, salvas.get(0).getUser());
        Assertions.assertTrue(salvas.get(0).getMensagem().contains("enviou R$ 150,00 para Rafael"));
        Assertions.assertEquals(rafael, salvas.get(1).getUser());
        Assertions.assertTrue(salvas.get(1).getMensagem().contains("recebeu R$ 150,00 de Georges"));
    }

    @Test
    void pagamentoSemDestinatarioNotificaSoORemetente() {
        service.notifyTransfer(transacao(usuario(1L, "Georges"), null));

        Mockito.verify(repository, Mockito.times(1)).save(Mockito.any(Notification.class));
    }

    @Test
    void estornoNotificaOsDoisLados() {
        User rafael = usuario(2L, "Rafael");
        User georges = usuario(1L, "Georges");

        service.notifyReversal(transacao(rafael, georges));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        Mockito.verify(repository, Mockito.times(2)).save(captor.capture());
        Assertions.assertEquals(georges, captor.getAllValues().get(0).getUser());
        Assertions.assertEquals(rafael, captor.getAllValues().get(1).getUser());
    }

    @Test
    void negarLeituraDeNotificacaoDeOutroUsuario() {
        Notification notificacao = new Notification(usuario(1L, "Georges"), "Aviso");
        when(repository.findById(5L)).thenReturn(Optional.of(notificacao));

        Assertions.assertThrows(ForbiddenException.class, () -> service.markAsRead(5L, 2L));
    }

    @Test
    void formatarValorEmReais() {
        Assertions.assertEquals("R$ 1.234,50", MoneyFormatter.format(new BigDecimal("1234.5")));
    }
}
