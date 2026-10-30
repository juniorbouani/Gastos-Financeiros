package com.georgesbouanni.controle_gastos.service;
import com.georgesbouanni.controle_gastos.dto.*;
import com.georgesbouanni.controle_gastos.model.*;
import com.georgesbouanni.controle_gastos.repository.*;
import com.georgesbouanni.controle_gastos.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ShopServiceTest {
 @Mock UserRepository users;@Mock ShopOrderRepository orders;@Mock CreditCardService cards;@InjectMocks ShopService service;
 User user(){User u=new User();u.setId(7L);return u;}
 CreditCard card(){CreditCard c=new CreditCard(user(),"**** 1234",new BigDecimal("1000.00"),false,LocalDate.now().plusYears(1));c.setId(10L);return c;}
 @Test void catalogRetainsServerPrices(){assertEquals(new BigDecimal("199.99"),service.product("500").preco());assertEquals(5,service.products().size());assertThrows(ResourceNotFoundException.class,()->service.product("inexistente"));}
 @Test void checkoutUsesCatalogPriceAndSelectedInstallments(){User u=user();CreditCard c=card();UUID key=UUID.randomUUID();when(users.findForShopUpdate(7L)).thenReturn(Optional.of(u));when(cards.findOwnedCard(10L,7L)).thenReturn(c);Invoice inv=new Invoice(c,YearMonth.now(),LocalDate.now().plusMonths(1));inv.setId(30L);InvoiceItem item=new InvoiceItem(inv,"Shop: JBL 500BT",new BigDecimal("66.66"),LocalDate.now(),1,3);when(cards.registerPurchase(10L,7L,"Shop: JBL 500BT",new BigDecimal("199.99"),3)).thenReturn(item);when(orders.save(any())).thenAnswer(i->i.getArgument(0));var result=service.checkout(7L,new ShopCheckoutRequest("500",10L,3,key));assertEquals(new BigDecimal("199.99"),result.total());assertEquals(30L,result.firstInvoiceId());verify(orders).save(any());}
 @Test void replayReturnsOriginalWithoutAnotherPurchase(){User u=user();CreditCard c=card();UUID key=UUID.randomUUID();ShopOrder existing=new ShopOrder(u,c,key.toString(),"500","JBL 500BT",new BigDecimal("199.99"),3,30L);when(users.findForShopUpdate(7L)).thenReturn(Optional.of(u));when(orders.findByUserIdAndRequestId(7L,key.toString())).thenReturn(Optional.of(existing));assertEquals(new BigDecimal("199.99"),service.checkout(7L,new ShopCheckoutRequest("500",10L,3,key)).total());verifyNoInteractions(cards);verify(orders,never()).save(any());}
 @Test void sameKeyCannotBeReusedForAnotherProduct(){User u=user();UUID key=UUID.randomUUID();when(users.findForShopUpdate(7L)).thenReturn(Optional.of(u));when(orders.findByUserIdAndRequestId(7L,key.toString())).thenReturn(Optional.of(new ShopOrder(u,card(),key.toString(),"500","JBL 500BT",new BigDecimal("199.99"),3,30L)));assertThrows(BusinessRuleException.class,()->service.checkout(7L,new ShopCheckoutRequest("mouse",10L,3,key)));verifyNoInteractions(cards);}
 @Test void expiredCardCannotPurchase(){CreditCard c=card();c.setDataExpiracao(LocalDate.now().minusDays(1));when(users.findForShopUpdate(7L)).thenReturn(Optional.of(user()));when(cards.findOwnedCard(10L,7L)).thenReturn(c);assertThrows(BusinessRuleException.class,()->service.checkout(7L,new ShopCheckoutRequest("500",10L,1,UUID.randomUUID())));verify(cards,never()).registerPurchase(any(),any(),any(),any(),anyInt());}
 @Test void foreignCardCannotPurchase(){when(users.findForShopUpdate(7L)).thenReturn(Optional.of(user()));when(cards.findOwnedCard(10L,7L)).thenThrow(new ForbiddenException("Cartão de outra conta"));assertThrows(ForbiddenException.class,()->service.checkout(7L,new ShopCheckoutRequest("500",10L,1,UUID.randomUUID())));verify(orders,never()).save(any());}

 @Test void disposableCardIsCancelledAfterSuccessfulShopPurchase(){User u=user();CreditCard c=card();c.setDescartavel(true);when(users.findForShopUpdate(7L)).thenReturn(Optional.of(u));when(cards.findOwnedCard(10L,7L)).thenReturn(c);Invoice inv=new Invoice(c,YearMonth.now(),LocalDate.now().plusMonths(1));inv.setId(30L);when(cards.registerPurchase(10L,7L,"Shop: JBL 500BT",new BigDecimal("199.99"),1)).thenReturn(new InvoiceItem(inv,"Shop",new BigDecimal("199.99"),LocalDate.now(),1,1));when(orders.save(any())).thenAnswer(i->i.getArgument(0));service.checkout(7L,new ShopCheckoutRequest("500",10L,1,UUID.randomUUID()));assertEquals(CreditCardStatus.CANCELADO,c.getStatus());}
}
