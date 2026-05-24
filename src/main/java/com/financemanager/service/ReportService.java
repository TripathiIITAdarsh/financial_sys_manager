package com.financemanager.service;

import com.financemanager.dto.ReportResponse;
import com.financemanager.entity.CategoryType;
import com.financemanager.entity.Transaction;
import com.financemanager.entity.User;
import com.financemanager.exception.BadRequestException;
import com.financemanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public ReportResponse monthlyReport(User user, int year, int month) {
        YearMonth ym;
        try {
            ym = YearMonth.of(year, month);
        } catch (DateTimeException e) {
            throw new BadRequestException("Invalid year or month");
        }
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        ReportResponse report = build(user, start, end);
        report.setMonth(month);
        report.setYear(year);
        return report;
    }

    public ReportResponse yearlyReport(User user, int year) {
        if (year < 1) {
            throw new BadRequestException("Invalid year");
        }
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        ReportResponse report = build(user, start, end);
        report.setYear(year);
        return report;
    }

    private ReportResponse build(User user, LocalDate start, LocalDate end) {
        List<Transaction> txs = transactionRepository.findByUserAndDateRange(user, start, end);
        Map<String, BigDecimal> income = new LinkedHashMap<>();
        Map<String, BigDecimal> expense = new LinkedHashMap<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (Transaction t : txs) {
            String name = t.getCategory().getName();
            if (t.getCategory().getType() == CategoryType.INCOME) {
                income.merge(name, t.getAmount(), BigDecimal::add);
                totalIncome = totalIncome.add(t.getAmount());
            } else {
                expense.merge(name, t.getAmount(), BigDecimal::add);
                totalExpense = totalExpense.add(t.getAmount());
            }
        }
        ReportResponse resp = new ReportResponse();
        resp.setTotalIncome(income);
        resp.setTotalExpenses(expense);
        resp.setNetSavings(totalIncome.subtract(totalExpense));
        return resp;
    }
}
