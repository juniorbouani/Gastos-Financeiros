package com.georgesbouanni.controle_gastos.service;

import com.georgesbouanni.controle_gastos.dto.ShopCheckoutRequest;
import com.georgesbouanni.controle_gastos.model.*;
import com.georgesbouanni.controle_gastos.repository.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ShopPersistenceTest {
    @Test
    void concurrentReplayCreatesOneOrderAndOnePurchase() throws Exception {
        var config =
                new Configuration()
                        .addAnnotatedClass(User.class)
                        .addAnnotatedClass(CreditCard.class)
                        .addAnnotatedClass(Invoice.class)
                        .addAnnotatedClass(InvoiceItem.class)
                        .addAnnotatedClass(Notification.class)
                        .addAnnotatedClass(ShopOrder.class);
        config.addAttributeConverter(YearMonthConverter.class, true);
        config.setProperty(
                "hibernate.connection.url",
                "jdbc:h2:mem:shop_idempotency;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000");
        config.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try (var factory = config.buildSessionFactory()) {
            var em = SharedEntityManagerCreator.createSharedEntityManager(factory);
            var repos = new JpaRepositoryFactory(em);
            UserRepository users = repos.getRepository(UserRepository.class);
            CreditCardRepository cards = repos.getRepository(CreditCardRepository.class);
            InvoiceRepository invoices = repos.getRepository(InvoiceRepository.class);
            InvoiceItemRepository items = repos.getRepository(InvoiceItemRepository.class);
            NotificationRepository notifications =
                    repos.getRepository(NotificationRepository.class);
            ShopOrderRepository orders = repos.getRepository(ShopOrderRepository.class);
            var cardService =
                    new CreditCardService(
                            cards, invoices, items, users, new NotificationService(notifications));
            var service = new ShopService(users, orders, cardService);
            var tx = new TransactionTemplate(new JpaTransactionManager(factory));
            Long[] ids =
                    tx.execute(
                            status -> {
                                User u =
                                        new User(
                                                "Teste",
                                                "12345678900",
                                                "11912345678",
                                                LocalDate.of(2000, 1, 1),
                                                "hash",
                                                "shop@example.invalid",
                                                BigDecimal.ZERO);
                                em.persist(u);
                                CreditCard c =
                                        new CreditCard(
                                                u,
                                                "**** 1234",
                                                new BigDecimal("1000.00"),
                                                false,
                                                LocalDate.now().plusYears(1));
                                em.persist(c);
                                em.flush();
                                return new Long[] {u.getId(), c.getId()};
                            });
            var request = new ShopCheckoutRequest("500", ids[1], 3, UUID.randomUUID());
            try (var executor = Executors.newFixedThreadPool(2)) {
                var first =
                        executor.submit(
                                () -> tx.execute(status -> service.checkout(ids[0], request)));
                var second =
                        executor.submit(
                                () -> tx.execute(status -> service.checkout(ids[0], request)));
                var a = first.get(20, TimeUnit.SECONDS);
                var b = second.get(20, TimeUnit.SECONDS);
                assertEquals(a.id(), b.id());
            }
            tx.executeWithoutResult(
                    status -> {
                        assertEquals(1, orders.count());
                        assertEquals(3, items.count());
                        assertEquals(3, invoices.count());
                        BigDecimal total =
                                items.findAll().stream()
                                        .map(InvoiceItem::getValor)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                        assertEquals(new BigDecimal("199.99"), total);
                        assertEquals(
                                new BigDecimal("800.01"),
                                cards.findById(ids[1]).orElseThrow().getLimiteDisponivel());
                        assertThrows(
                                com.georgesbouanni.controle_gastos.exception
                                        .ResourceNotFoundException.class,
                                () -> service.findRequest(999L, request.requestId()));
                    });
        }
    }
}
