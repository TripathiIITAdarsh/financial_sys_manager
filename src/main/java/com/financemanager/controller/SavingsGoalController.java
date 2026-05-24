package com.financemanager.controller;

import com.financemanager.dto.SavingsGoalRequest;
import com.financemanager.dto.SavingsGoalResponse;
import com.financemanager.dto.SavingsGoalUpdateRequest;
import com.financemanager.security.CurrentUserProvider;
import com.financemanager.service.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {

    private final SavingsGoalService goalService;
    private final CurrentUserProvider currentUserProvider;

    public SavingsGoalController(SavingsGoalService goalService, CurrentUserProvider currentUserProvider) {
        this.goalService = goalService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<SavingsGoalResponse> create(@Valid @RequestBody SavingsGoalRequest request) {
        SavingsGoalResponse response = goalService.create(currentUserProvider.getCurrentUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        List<SavingsGoalResponse> goals = goalService.list(currentUserProvider.getCurrentUser());
        Map<String, Object> body = new HashMap<>();
        body.put("goals", goals);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(goalService.get(currentUserProvider.getCurrentUser(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody SavingsGoalUpdateRequest request) {
        return ResponseEntity.ok(goalService.update(currentUserProvider.getCurrentUser(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        goalService.delete(currentUserProvider.getCurrentUser(), id);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Goal deleted successfully");
        return ResponseEntity.ok(body);
    }
}
