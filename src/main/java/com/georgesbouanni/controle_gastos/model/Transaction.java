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

    @NotBlank(message =  "A descrição é obrigatória!")
    private String description;

    @NotNull (message = "O valor é obrigatório!")
    @Positive(message = "O valor deve ser positivo!")
    private BigDecimal value;

    @NotNull (message = "A data é obrigatória!")
    private LocalDate date;

    @NotNull(message = "O tipo é obrigatorio!")
    private TransactionType type;

    private String category;

    public Transaction() {
    }

    public Transaction(String id, String description, BigDecimal value, LocalDate date, TransactionType type, String category) {
        this.id = id;
        this.description = description;
        this.value = value;
        this.date = date;
        this.type = type;
        this.category = category;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
