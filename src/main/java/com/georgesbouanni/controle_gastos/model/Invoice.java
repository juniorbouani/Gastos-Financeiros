package com.georgesbouanni.controle_gastos.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O cartão é obrigatório")
    @ManyToOne
    @JoinColumn(name = "credit_card_id")
    private CreditCard creditCard;

    @NotNull(message = "O mês de referência é obrigatório")
    private YearMonth mesReferencia;

    private BigDecimal valorTotal = BigDecimal.ZERO;

    private BigDecimal valorPago = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @NotNull(message = "A data de vencimento é obrigatória")
    private LocalDate dataVencimento;

    public Invoice() {
    }

    public Invoice(Long id, CreditCard creditCard, YearMonth mesReferencia, BigDecimal valorTotal, BigDecimal valorPago, InvoiceStatus status, LocalDate dataVencimento) {
        this.id = id;
        this.creditCard = creditCard;
        this.mesReferencia = mesReferencia;
        this.valorTotal = valorTotal;
        this.valorPago = valorPago;
        this.status = status;
        this.dataVencimento = dataVencimento;
    }

    public Invoice(CreditCard creditCard, YearMonth mesReferencia, LocalDate dataVencimento) {
        this.creditCard = creditCard;
        this.mesReferencia = mesReferencia;
        this.dataVencimento = dataVencimento;
        this.status = InvoiceStatus.ABERTA;
        this.valorTotal = BigDecimal.ZERO;
        this.valorPago = BigDecimal.ZERO;
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CreditCard getCreditCard() {
        return creditCard;
    }

    public void setCreditCard(CreditCard creditCard) {
        this.creditCard = creditCard;
    }

    public YearMonth getMesReferencia() {
        return mesReferencia;
    }

    public void setMesReferencia(YearMonth mesReferencia) {
        this.mesReferencia = mesReferencia;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public void setValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }
}
