package com.georgesbouanni.controle_gastos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "A descrição é obrigatória!")
    private String description;

    @NotNull(message = "O valor é obrigatório!")
    @Positive(message = "O valor deve ser positivo!")
    private BigDecimal value;

    @NotNull(message = "A data é obrigatória!")
    private LocalDate date;

    @NotNull(message = "O tipo é obrigatorio!")
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @NotNull(message = "O status é obrigatorio")
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @NotNull(message = "O remetente é obrigatório!")
    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    private String destination;

    public Transaction() {
    }

    public Transaction(String description, BigDecimal value, LocalDate date, TransactionType type,
                       TransactionStatus status, User sender, User receiver, String destination) {
        this.description = description;
        this.value = value;
        this.date = date;
        this.type = type;
        this.status = status;
        this.sender = sender;
        this.receiver = receiver;
        this.destination = destination;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}