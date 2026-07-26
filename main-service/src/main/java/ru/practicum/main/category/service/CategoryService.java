package ru.practicum.main.category.service;

import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.dto.NewCategoryDto;

public interface CategoryService {
    CategoryDto createCategory(NewCategoryDto category);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(CategoryDto category, long catId);
}
