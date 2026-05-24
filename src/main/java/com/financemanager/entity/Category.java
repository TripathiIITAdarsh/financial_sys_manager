package com.financemanager.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "user_id"}))
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    @Column(nullable = false)
    private boolean isCustom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Category() {}

    public Category(String name, CategoryType type, boolean isCustom, User user) {
        this.name = name;
        this.type = type;
        this.isCustom = isCustom;
        this.user = user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CategoryType getType() { return type; }
    public void setType(CategoryType type) { this.type = type; }
    public boolean isCustom() { return isCustom; }
    public void setCustom(boolean custom) { isCustom = custom; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
