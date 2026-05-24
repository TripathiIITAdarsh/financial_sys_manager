package com.financemanager.service;

import com.financemanager.dto.CategoryRequest;
import com.financemanager.dto.CategoryResponse;
import com.financemanager.entity.Category;
import com.financemanager.entity.User;
import com.financemanager.exception.BadRequestException;
import com.financemanager.exception.ConflictException;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.exception.NotFoundException;
import com.financemanager.repository.CategoryRepository;
import com.financemanager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<CategoryResponse> listForUser(User user) {
        List<Category> all = new ArrayList<>(categoryRepository.findDefaultsAndForUser(user));
        all.sort(Comparator.comparing(Category::isCustom).thenComparing(Category::getName));
        List<CategoryResponse> out = new ArrayList<>(all.size());
        for (Category c : all) {
            out.add(new CategoryResponse(c.getName(), c.getType(), c.isCustom()));
        }
        return out;
    }

    @Transactional
    public CategoryResponse createCustomCategory(User user, CategoryRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Category name is required");
        }
        if (request.getType() == null) {
            throw new BadRequestException("Category type is required");
        }
        if (categoryRepository.findByNameAndUserIsNull(request.getName()).isPresent()) {
            throw new ConflictException("Category name conflicts with a default category");
        }
        if (categoryRepository.existsByNameAndUser(request.getName(), user)) {
            throw new ConflictException("Custom category with this name already exists");
        }
        Category category = new Category(request.getName(), request.getType(), true, user);
        Category saved = categoryRepository.save(category);
        return new CategoryResponse(saved.getName(), saved.getType(), saved.isCustom());
    }

    @Transactional
    public void deleteCustomCategory(User user, String name) {
        Optional<Category> defaultMatch = categoryRepository.findByNameAndUserIsNull(name);
        if (defaultMatch.isPresent()) {
            throw new ForbiddenException("Default categories cannot be deleted");
        }
        Category category = categoryRepository.findByNameAndUser(name, user)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        if (transactionRepository.existsByCategory(category)) {
            throw new BadRequestException("Category is referenced by transactions and cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    /**
     * Resolves a category by name for a user. Looks first at the user's custom categories then defaults.
     */
    public Category resolveCategory(User user, String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Category is required");
        }
        return categoryRepository.findByNameAndUser(name, user)
                .or(() -> categoryRepository.findByNameAndUserIsNull(name))
                .orElseThrow(() -> new BadRequestException("Invalid category: " + name));
    }
}
