package com.financemanager.service;

import com.financemanager.dto.TransactionRequest;
import com.financemanager.dto.TransactionResponse;
import com.financemanager.dto.TransactionUpdateRequest;
import com.financemanager.entity.Category;
import com.financemanager.entity.Transaction;
import com.financemanager.entity.User;
import com.financemanager.exception.BadRequestException;
import com.financemanager.exception.NotFoundException;
import com.financemanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    @Transactional
    public TransactionResponse create(User user, TransactionRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be positive");
        }
        if (request.getDate() == null) {
            throw new BadRequestException("Date is required");
        }
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Date cannot be a future date");
        }
        Category category = categoryService.resolveCategory(user, request.getCategory());

        Transaction tx = new Transaction();
        tx.setAmount(request.getAmount());
        tx.setDate(request.getDate());
        tx.setCategory(category);
        tx.setDescription(request.getDescription());
        tx.setUser(user);
        Transaction saved = transactionRepository.save(tx);
        return toResponse(saved);
    }

    public List<TransactionResponse> list(User user, LocalDate startDate, LocalDate endDate, Long categoryId) {
        List<Transaction> all = transactionRepository.findByUserOrderByDateDescIdDesc(user);
        List<TransactionResponse> out = new ArrayList<>();
        for (Transaction t : all) {
            if (startDate != null && t.getDate().isBefore(startDate)) continue;
            if (endDate != null && t.getDate().isAfter(endDate)) continue;
            if (categoryId != null && !categoryId.equals(t.getCategory().getId())) continue;
            out.add(toResponse(t));
        }
        return out;
    }

    @Transactional
    public TransactionResponse update(User user, Long id, TransactionUpdateRequest request) {
        Transaction tx = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Amount must be positive");
            }
            tx.setAmount(request.getAmount());
        }
        if (request.getCategory() != null) {
            Category category = categoryService.resolveCategory(user, request.getCategory());
            tx.setCategory(category);
        }
        if (request.getDescription() != null) {
            tx.setDescription(request.getDescription());
        }
        return toResponse(tx);
    }

    @Transactional
    public void delete(User user, Long id) {
        Transaction tx = transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        transactionRepository.delete(tx);
    }

    public TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAmount(),
                t.getDate(),
                t.getCategory().getName(),
                t.getDescription(),
                t.getCategory().getType().name()
        );
    }
}
