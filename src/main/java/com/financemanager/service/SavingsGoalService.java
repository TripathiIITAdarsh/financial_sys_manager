package com.financemanager.service;

import com.financemanager.dto.SavingsGoalRequest;
import com.financemanager.dto.SavingsGoalResponse;
import com.financemanager.dto.SavingsGoalUpdateRequest;
import com.financemanager.entity.CategoryType;
import com.financemanager.entity.SavingsGoal;
import com.financemanager.entity.Transaction;
import com.financemanager.entity.User;
import com.financemanager.exception.BadRequestException;
import com.financemanager.exception.NotFoundException;
import com.financemanager.repository.SavingsGoalRepository;
import com.financemanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SavingsGoalService {

    private final SavingsGoalRepository goalRepository;
    private final TransactionRepository transactionRepository;

    public SavingsGoalService(SavingsGoalRepository goalRepository,
                              TransactionRepository transactionRepository) {
        this.goalRepository = goalRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public SavingsGoalResponse create(User user, SavingsGoalRequest request) {
        validateCreate(request);
        SavingsGoal goal = new SavingsGoal();
        goal.setGoalName(request.getGoalName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());
        goal.setUser(user);
        SavingsGoal saved = goalRepository.save(goal);
        return toResponse(saved);
    }

    private void validateCreate(SavingsGoalRequest request) {
        if (request.getGoalName() == null || request.getGoalName().isBlank()) {
            throw new BadRequestException("Goal name is required");
        }
        if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Target amount must be positive");
        }
        if (request.getTargetDate() == null || !request.getTargetDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Target date must be a future date");
        }
    }

    public List<SavingsGoalResponse> list(User user) {
        List<SavingsGoal> goals = goalRepository.findByUser(user);
        List<SavingsGoalResponse> out = new ArrayList<>(goals.size());
        for (SavingsGoal g : goals) {
            out.add(toResponse(g));
        }
        return out;
    }

    public SavingsGoalResponse get(User user, Long id) {
        SavingsGoal goal = goalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Savings goal not found"));
        return toResponse(goal);
    }

    @Transactional
    public SavingsGoalResponse update(User user, Long id, SavingsGoalUpdateRequest request) {
        SavingsGoal goal = goalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Savings goal not found"));
        if (request.getTargetAmount() != null) {
            if (request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Target amount must be positive");
            }
            goal.setTargetAmount(request.getTargetAmount());
        }
        if (request.getTargetDate() != null) {
            if (!request.getTargetDate().isAfter(LocalDate.now())) {
                throw new BadRequestException("Target date must be a future date");
            }
            goal.setTargetDate(request.getTargetDate());
        }
        return toResponse(goal);
    }

    @Transactional
    public void delete(User user, Long id) {
        SavingsGoal goal = goalRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Savings goal not found"));
        goalRepository.delete(goal);
    }

    private SavingsGoalResponse toResponse(SavingsGoal goal) {
        BigDecimal progress = calculateProgress(goal);
        BigDecimal percentage = goal.getTargetAmount().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : progress.multiply(BigDecimal.valueOf(100))
                        .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
        BigDecimal remaining = goal.getTargetAmount().subtract(progress);

        SavingsGoalResponse resp = new SavingsGoalResponse();
        resp.setId(goal.getId());
        resp.setGoalName(goal.getGoalName());
        resp.setTargetAmount(goal.getTargetAmount());
        resp.setTargetDate(goal.getTargetDate());
        resp.setStartDate(goal.getStartDate());
        resp.setCurrentProgress(progress);
        resp.setProgressPercentage(percentage);
        resp.setRemainingAmount(remaining);
        return resp;
    }

    private BigDecimal calculateProgress(SavingsGoal goal) {
        LocalDate end = LocalDate.now().isAfter(goal.getTargetDate()) ? goal.getTargetDate() : LocalDate.now();
        if (goal.getStartDate().isAfter(end)) {
            return BigDecimal.ZERO;
        }
        List<Transaction> txs = transactionRepository.findByUserAndDateRange(goal.getUser(),
                goal.getStartDate(), end);
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (Transaction t : txs) {
            if (t.getCategory().getType() == CategoryType.INCOME) {
                income = income.add(t.getAmount());
            } else {
                expense = expense.add(t.getAmount());
            }
        }
        return income.subtract(expense);
    }
}
