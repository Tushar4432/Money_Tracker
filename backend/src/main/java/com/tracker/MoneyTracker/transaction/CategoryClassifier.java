package com.tracker.MoneyTracker.transaction;

import org.springframework.stereotype.Component;

@Component
public class CategoryClassifier {

    private final CategoryMapper categoryMapper;

    public CategoryClassifier() {
        this.categoryMapper = new CategoryMapper();
    }

    public CategoryClassifier(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public String classify(String details) {
        return categoryMapper.getCategory(details);
    }
}

