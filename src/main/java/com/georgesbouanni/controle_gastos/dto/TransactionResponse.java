package com.georgesbouanni.controle_gastos.dto;

import com.georgesbouanni.controle_gastos.model.Transaction;
import com.georgesbouanni.controle_gastos.model.TransactionStatus;
import com.georgesbouanni.controle_gastos.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionResponse {

    private final Long id;
    private final String description;
    private final BigDecimal value;
    private final LocalDate date;
    private final TransactionType type;
    private final TransactionStatus status;
    private final PartyResponse sender;
    private final PartyResponse receiver;
    private final String destination;
    private final Long categoryId;
    private final String categoryName;

    public TransactionResponse(Transaction t) {
        this.id = t.getId();
        this.description = t.getDescription();
        this.value = t.getValue();
        this.date = t.getDate();
        this.type = t.getType();
        this.status = t.getStatus();
        this.sender = PartyResponse.from(t.getSender());
        this.receiver = PartyResponse.from(t.getReceiver());
        this.destination = t.getDestination();
        this.categoryId = t.getCategory() != null ? t.getCategory().getId() : null ;
        this.categoryName = t.getCategory() != null ? t.getCategory().getName() : null;
    }

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(transaction);
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getValue() {
        return value;
    }

    public LocalDate getDate() {
        return date;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public PartyResponse getSender() {
        return sender;
    }

    public PartyResponse getReceiver() {
        return receiver;
    }

    public String getDestination() {
        return destination;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }
}
