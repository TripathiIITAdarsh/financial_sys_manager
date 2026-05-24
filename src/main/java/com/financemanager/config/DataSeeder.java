package com.financemanager.config;

import com.financemanager.entity.Category;
import com.financemanager.entity.CategoryType;
import com.financemanager.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataSeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.findByUserIsNull().isEmpty()) {
            categoryRepository.saveAll(List.of(
                    new Category("Salary", CategoryType.INCOME, false, null),
                    new Category("Food", CategoryType.EXPENSE, false, null),
                    new Category("Rent", CategoryType.EXPENSE, false, null),
                    new Category("Transportation", CategoryType.EXPENSE, false, null),
                    new Category("Entertainment", CategoryType.EXPENSE, false, null),
                    new Category("Healthcare", CategoryType.EXPENSE, false, null),
                    new Category("Utilities", CategoryType.EXPENSE, false, null)
            ));
        }
    }
}
