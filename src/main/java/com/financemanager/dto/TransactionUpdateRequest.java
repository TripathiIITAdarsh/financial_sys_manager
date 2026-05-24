package com.financemanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionUpdateRequest {
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String category;

    private String description;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
