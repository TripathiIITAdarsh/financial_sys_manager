package com.financemanager.controller;

import com.financemanager.dto.CategoryRequest;
import com.financemanager.dto.CategoryResponse;
import com.financemanager.security.CurrentUserProvider;
import com.financemanager.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CurrentUserProvider currentUserProvider;

    public CategoryController(CategoryService categoryService, CurrentUserProvider currentUserProvider) {
        this.categoryService = categoryService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        List<CategoryResponse> categories = categoryService.listForUser(currentUserProvider.getCurrentUser());
        Map<String, Object> body = new HashMap<>();
        body.put("categories", categories);
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCustomCategory(
                currentUserProvider.getCurrentUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String name) {
        categoryService.deleteCustomCategory(currentUserProvider.getCurrentUser(), name);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Category deleted successfully");
        return ResponseEntity.ok(body);
    }
}
