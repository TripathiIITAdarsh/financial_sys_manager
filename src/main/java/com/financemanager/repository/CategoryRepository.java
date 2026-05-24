package com.financemanager.repository;

import com.financemanager.entity.Category;
import com.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    /**
     * Default (system) categories have user = null.
     */
    List<Category> findByUserIsNull();

    @Query("SELECT c FROM Category c WHERE c.user IS NULL OR c.user = :user")
    List<Category> findDefaultsAndForUser(@Param("user") User user);

    Optional<Category> findByNameAndUserIsNull(String name);

    Optional<Category> findByNameAndUser(String name, User user);

    boolean existsByNameAndUser(String name, User user);
}
