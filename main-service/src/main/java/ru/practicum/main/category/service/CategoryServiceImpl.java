package ru.practicum.main.category.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.dto.CategoryMapper;
import ru.practicum.main.category.dto.NewCategoryDto;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

@Service
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper mapper;

    @Override
    public CategoryDto createCategory(NewCategoryDto category) {
        if (categoryRepository.findByName(category.getName()) != null) {
            throw new ConflictException("Данная категория уже существует!");
        }
        Category ct = mapper.toEntity(category);
        return mapper.toDto(categoryRepository.save(ct));
    }

    @Override
    public void deleteCategory(Long catId) {
        checkAndReturnCategory(catId);
        if (eventRepository.countEventBySelectCategory(catId) != 0L) {
            throw new ConflictException("Категория еще используется каким-то событием!");
        }
        categoryRepository.deleteById(catId);
    }

    @Override
    public CategoryDto updateCategory(CategoryDto category, long catId) {
        if (categoryRepository.findByName(category.getName()) != null) {
            throw new ConflictException("Данная категория уже существует!");
        }
        Category ct = checkAndReturnCategory(catId);
        ct.setName(category.getName());
        return mapper.toDto(categoryRepository.save(ct));
    }

    private Category checkAndReturnCategory(long catId) {
        return categoryRepository.findById(catId).orElseThrow(() ->
            new NotFoundException("Категория с id = " + catId + " не найдена!"));
        }
}
