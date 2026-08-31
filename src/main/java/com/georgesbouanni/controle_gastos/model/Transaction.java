package com.georgesbouanni.controle_gastos.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    @NotBlank(message = "A descrição é obrigatória!")
    private String description;

    @NotNull(message = "O valor é obrigatório!")
    @Positive(message = "O valor deve ser positivo!")
    private BigDecimal value;

    @NotNull(message = "A data é obrigatória!")
    private LocalDate date;

    @NotNull(message = "O tipo é obrigatorio!")
    private TransactionType type;

    @NotNull(message = "O status é obrigatorio")
    private TransactionStatus status;

    @NotBlank(message = "O remetente é obrigatório!")
    private String senderId;

    private String receiverId;

    private String destination;

    public Transaction() {
    }

    public Transaction(String description, BigDecimal value, LocalDate date, TransactionType type,
                       TransactionStatus status, String senderId, String receiverId, String destination) {
        this.description = description;
        this.value = value;
        this.date = date;
        this.type = type;
        this.status = status;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.destination = destination;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}