package com.georgesbouanni.controle_gastos.dto;

import com.georgesbouanni.controle_gastos.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionRequest {

    @NotBlank(message = "A descrição é obrigatória!")
    private String description;

    @NotNull(message = "O valor é obrigatório!")
    @Positive(message = "O valor deve ser positivo!")
    private BigDecimal value;

    @NotNull(message = "A data é obrigatória!")
    private LocalDate date;

    @NotNull(message = "O tipo é obrigatório!")
    private TransactionType type;

    @NotNull(message = "O remetente é obrigatório!")
    private Long senderId;

    private Long receiverId;

    private String destination;


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

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}