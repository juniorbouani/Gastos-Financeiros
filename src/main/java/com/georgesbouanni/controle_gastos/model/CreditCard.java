package com.georgesbouanni.controle_gastos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "credit_cards")
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @NotNull(message = "O usuário é obrigatório")
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String numeroMascarado;

    @NotNull(message = "O limite total é obrigatório")
    @PositiveOrZero
    private BigDecimal limiteTotal;

    @NotNull(message = "O limite disponível é obrigatório")
    @PositiveOrZero
    private BigDecimal limiteDisponivel;

    @Enumerated(EnumType.STRING)
    private CreditCardStatus status;

    private boolean descartavel;

    private LocalDate dataExpiracao;

    @JsonIgnore
    @Version
    @Column(name = "row_version", nullable = false, columnDefinition = "bigint default 0")
    private Long rowVersion = 0L;

    public CreditCard() {
    }

    public CreditCard(User user, String numeroMascarado, BigDecimal limiteTotal, boolean descartavel, LocalDate dataExpiracao) {
        this.user = user;
        this.numeroMascarado = numeroMascarado;
        this.limiteTotal = limiteTotal;
        this.limiteDisponivel = limiteTotal;
        this.status = CreditCardStatus.ATIVO;
        this.descartavel = descartavel;
        this.dataExpiracao = dataExpiracao;
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

    public String getNumeroMascarado() {
        return numeroMascarado;
    }

    public void setNumeroMascarado(String numeroMascarado) {
        this.numeroMascarado = numeroMascarado;
    }

    public BigDecimal getLimiteTotal() {
        return limiteTotal;
    }

    public void setLimiteTotal(BigDecimal limiteTotal) {
        this.limiteTotal = limiteTotal;
    }

    public BigDecimal getLimiteDisponivel() {
        return limiteDisponivel;
    }

    public void setLimiteDisponivel(BigDecimal limiteDisponivel) {
        this.limiteDisponivel = limiteDisponivel;
    }

    public CreditCardStatus getStatus() {
        return status;
    }

    public void setStatus(CreditCardStatus status) {
        this.status = status;
    }

    public boolean isDescartavel() {
        return descartavel;
    }

    public void setDescartavel(boolean descartavel) {
        this.descartavel = descartavel;
    }

    public LocalDate getDataExpiracao() {
        return dataExpiracao;
    }

    public void setDataExpiracao(LocalDate dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }
}
