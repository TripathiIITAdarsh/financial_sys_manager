package com.financemanager.dto;

import com.financemanager.entity.CategoryType;

public class CategoryResponse {
    private String name;
    private CategoryType type;
    private boolean isCustom;

    public CategoryResponse() {}

    public CategoryResponse(String name, CategoryType type, boolean isCustom) {
        this.name = name;
        this.type = type;
        this.isCustom = isCustom;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CategoryType getType() { return type; }
    public void setType(CategoryType type) { this.type = type; }
    public boolean getIsCustom() { return isCustom; }
    public void setIsCustom(boolean isCustom) { this.isCustom = isCustom; }
}
