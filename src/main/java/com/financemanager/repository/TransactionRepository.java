package com.financemanager.repository;

import com.financemanager.entity.Category;
import com.financemanager.entity.Transaction;
import com.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByDateDescIdDesc(User user);

    Optional<Transaction> findByIdAndUser(Long id, User user);

    boolean existsByCategory(Category category);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
           "AND t.date >= :startDate AND t.date <= :endDate")
    List<Transaction> findByUserAndDateRange(@Param("user") User user,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
}
