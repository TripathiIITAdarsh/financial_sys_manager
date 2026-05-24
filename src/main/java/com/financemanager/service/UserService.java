package com.financemanager.service;

import com.financemanager.dto.RegisterRequest;
import com.financemanager.entity.Category;
import com.financemanager.entity.CategoryType;
import com.financemanager.entity.User;
import com.financemanager.exception.ConflictException;
import com.financemanager.repository.CategoryRepository;
import com.financemanager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       CategoryRepository categoryRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already registered");
        }
        ensureDefaultCategoriesSeeded();
        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhoneNumber()
        );
        return userRepository.save(user);
    }

    private void ensureDefaultCategoriesSeeded() {
        if (!categoryRepository.findByUserIsNull().isEmpty()) {
            return;
        }
        List<Category> defaults = List.of(
                new Category("Salary", CategoryType.INCOME, false, null),
                new Category("Food", CategoryType.EXPENSE, false, null),
                new Category("Rent", CategoryType.EXPENSE, false, null),
                new Category("Transportation", CategoryType.EXPENSE, false, null),
                new Category("Entertainment", CategoryType.EXPENSE, false, null),
                new Category("Healthcare", CategoryType.EXPENSE, false, null),
                new Category("Utilities", CategoryType.EXPENSE, false, null)
        );
        categoryRepository.saveAll(defaults);
    }
}
