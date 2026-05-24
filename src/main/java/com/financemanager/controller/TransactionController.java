package com.financemanager.controller;

import com.financemanager.dto.TransactionRequest;
import com.financemanager.dto.TransactionResponse;
import com.financemanager.dto.TransactionUpdateRequest;
import com.financemanager.security.CurrentUserProvider;
import com.financemanager.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final CurrentUserProvider currentUserProvider;

    public TransactionController(TransactionService transactionService,
                                 CurrentUserProvider currentUserProvider) {
        this.transactionService = transactionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.create(currentUserProvider.getCurrentUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId) {
        List<TransactionResponse> transactions = transactionService.list(
                currentUserProvider.getCurrentUser(), startDate, endDate, categoryId);
        Map<String, Object> body = new HashMap<>();
        body.put("transactions", transactions);
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody TransactionUpdateRequest request) {
        return ResponseEntity.ok(
                transactionService.update(currentUserProvider.getCurrentUser(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        transactionService.delete(currentUserProvider.getCurrentUser(), id);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Transaction deleted successfully");
        return ResponseEntity.ok(body);
    }
}
