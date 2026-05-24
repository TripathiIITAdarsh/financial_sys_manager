package com.financemanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SavingsGoalResponse {
    private Long id;
    private String goalName;
    private BigDecimal targetAmount;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    private BigDecimal currentProgress;
    private BigDecimal progressPercentage;
    private BigDecimal remainingAmount;

    public SavingsGoalResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGoalName() { return goalName; }
    public void setGoalName(String goalName) { this.goalName = goalName; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public BigDecimal getCurrentProgress() { return currentProgress; }
    public void setCurrentProgress(BigDecimal currentProgress) { this.currentProgress = currentProgress; }
    public BigDecimal getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(BigDecimal progressPercentage) { this.progressPercentage = progressPercentage; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }
}
