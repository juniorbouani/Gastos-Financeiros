package com.georgesbouanni.controle_gastos.service;
import com.georgesbouanni.controle_gastos.model.User;
import com.georgesbouanni.controle_gastos.model.CreditCard;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class AccountSchemaTest {
 @Test void schemaSupportsClosureAndRejectsStaleBalanceUpdates() {
  var configuration=new Configuration().addAnnotatedClass(User.class).addAnnotatedClass(CreditCard.class);
  configuration.setProperty("hibernate.connection.url","jdbc:h2:mem:account_security;DB_CLOSE_DELAY=-1");
  configuration.setProperty("hibernate.connection.driver_class","org.h2.Driver");
  configuration.setProperty("hibernate.hbm2ddl.auto","create-drop");
  try(var factory=configuration.buildSessionFactory()) {
   Long id;
   try(var session=factory.openSession()) {var tx=session.beginTransaction();User u=new User("Teste","12345678900","11912345678",LocalDate.of(2000,1,1),"hash","test@example.invalid",BigDecimal.ZERO);session.persist(u);tx.commit();id=u.getId();}
   try(var stale=factory.openSession()) {
    var staleTx=stale.beginTransaction();User old=stale.find(User.class,id);assertFalse(old.isAccountClosed());assertEquals(0,old.getCredentialVersion());
    try(var current=factory.openSession()){var tx=current.beginTransaction();User u=current.find(User.class,id);u.setAccountClosed(true);u.setCredentialVersion(1);tx.commit();}
    old.setBalance(BigDecimal.TEN);
    assertThrows(jakarta.persistence.OptimisticLockException.class,staleTx::commit);
   }
   try(var session=factory.openSession()){User saved=session.find(User.class,id);assertTrue(saved.isAccountClosed());assertEquals(0,saved.getBalance().signum());assertEquals(1,saved.getCredentialVersion());}
  }
 }
}
