package com.georgesbouanni.controle_gastos.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(name="shop_orders",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","request_id"}))
public class ShopOrder {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @ManyToOne(optional=false) @JoinColumn(name="card_id",nullable=false) private CreditCard card;
 @Column(name="request_id",nullable=false,length=36) private String requestId;
 @Column(nullable=false) private String productId;
 @Column(nullable=false) private String productName;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal total;
 @Column(nullable=false) private int totalParcelas;
 @Column(nullable=false) private LocalDateTime createdAt;
 private Long firstInvoiceId;
 public ShopOrder() {}
 public ShopOrder(User user,CreditCard card,String requestId,String productId,String productName,BigDecimal total,int totalParcelas,Long firstInvoiceId) {
  this.user=user;this.card=card;this.requestId=requestId;this.productId=productId;this.productName=productName;this.total=total;this.totalParcelas=totalParcelas;this.firstInvoiceId=firstInvoiceId;this.createdAt=LocalDateTime.now();
 }
 public Long getId(){return id;} public User getUser(){return user;} public CreditCard getCard(){return card;}
 public String getRequestId(){return requestId;} public String getProductId(){return productId;} public String getProductName(){return productName;}
 public BigDecimal getTotal(){return total;} public int getTotalParcelas(){return totalParcelas;} public LocalDateTime getCreatedAt(){return createdAt;} public Long getFirstInvoiceId(){return firstInvoiceId;}
}
