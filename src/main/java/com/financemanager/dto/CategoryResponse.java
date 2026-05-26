package com.financemanager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.financemanager.entity.CategoryType;

public class CategoryResponse {
    private String name;
    private CategoryType type;
    @JsonProperty("custom")
    private boolean custom;

    public CategoryResponse() {}

    public CategoryResponse(String name, CategoryType type, boolean custom) {
        this.name = name;
        this.type = type;
        this.custom = custom;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CategoryType getType() { return type; }
    public void setType(CategoryType type) { this.type = type; }
    @JsonProperty("custom")
    public boolean isCustom() { return custom; }
    public void setCustom(boolean custom) { this.custom = custom; }
}
