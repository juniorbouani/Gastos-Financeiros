package com.georgesbouanni.controle_gastos.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoce_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "A fatura é obrigatória")
    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @NotNull(message = "A descrição é obrigatória")
    private  String descricao;

    @NotNull(message = "O valor é obrigatório")
    @Positive(message = "O valor deve ser positivo")
    private BigDecimal  valor;

    @NotNull(message = "A data é obrigatória")
    private LocalDate data;

    private int parcelaAtual = 1;

    private int totalParcelas= 1;

    private boolean estornado = false;

    public InvoiceItem() {
    }

    public InvoiceItem(Long id, Invoice invoice, String descricao, BigDecimal valor, LocalDate data, int parcelaAtual, int totalParcelas, boolean estornado) {
        this.id = id;
        this.invoice = invoice;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.parcelaAtual = parcelaAtual;
        this.totalParcelas = totalParcelas;
        this.estornado = estornado;
    }

    public InvoiceItem(Invoice invoice, String descricao, BigDecimal valor, LocalDate data, int parcelaAtual, int totalParcelas) {
        this.invoice = invoice;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.parcelaAtual = parcelaAtual;
        this.totalParcelas = totalParcelas;
        this.estornado = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public int getParcelaAtual() {
        return parcelaAtual;
    }

    public void setParcelaAtual(int parcelaAtual) {
        this.parcelaAtual = parcelaAtual;
    }

    public int getTotalParcelas() {
        return totalParcelas;
    }

    public void setTotalParcelas(int totalParcelas) {
        this.totalParcelas = totalParcelas;
    }

    public boolean isEstornado() {
        return estornado;
    }

    public void setEstornado(boolean estornado) {
        this.estornado = estornado;
    }
}
