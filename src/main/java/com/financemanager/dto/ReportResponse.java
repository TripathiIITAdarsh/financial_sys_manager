package com.financemanager.dto;

import java.math.BigDecimal;
import java.util.Map;

public class ReportResponse {
    private Integer month;
    private Integer year;
    private Map<String, BigDecimal> totalIncome;
    private Map<String, BigDecimal> totalExpenses;
    private BigDecimal netSavings;

    public ReportResponse() {}

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Map<String, BigDecimal> getTotalIncome() { return totalIncome; }
    public void setTotalIncome(Map<String, BigDecimal> totalIncome) { this.totalIncome = totalIncome; }
    public Map<String, BigDecimal> getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(Map<String, BigDecimal> totalExpenses) { this.totalExpenses = totalExpenses; }
    public BigDecimal getNetSavings() { return netSavings; }
    public void setNetSavings(BigDecimal netSavings) { this.netSavings = netSavings; }
}
