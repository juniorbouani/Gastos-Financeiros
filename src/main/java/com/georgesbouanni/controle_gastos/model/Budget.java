package com.georgesbouanni.controle_gastos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Table(name = "budgets", uniqueConstraints = { @UniqueConstraint(columnNames = {"user_id", "category_id", "mes_referencia" })
})
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O usuário é obrigatório!")
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull(message = "A categoria é obrigatória!")
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @NotNull(message = "O mês referência é obrigatório!")
    private YearMonth mesReferencia;

    @NotNull(message = "O limite é obrigatório!")
    @Positive(message = "O limite deve ser positivo!")
    private BigDecimal limite;

    public Budget() {
    }

    public Budget(User user, Category category, YearMonth mesReferencia, BigDecimal limite) {
        this.user = user;
        this.category = category;
        this.mesReferencia = mesReferencia;
        this.limite = limite;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public YearMonth getMesReferencia() {
        return mesReferencia;
    }

    public void setMesReferencia(YearMonth mesReferencia) {
        this.mesReferencia = mesReferencia;
    }

    public BigDecimal getLimite() {
        return limite;
    }

    public void setLimite(BigDecimal limite) {
        this.limite = limite;
    }
}

