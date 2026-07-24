package ru.practicum.main.category.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.dto.NewCategoryDto;
import ru.practicum.main.category.service.CategoryService;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;

@AllArgsConstructor
@RequestMapping("/admin/categories")
@RestController
public class AdminCategoryController {
    private final CategoryService categoryService;
    private final StatsClient statsClient;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody NewCategoryDto category,
                                      HttpServletRequest request) {
        sendHit(request.getRemoteAddr(), request.getRequestURI());
        return categoryService.createCategory(category);
    }

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable long catId,
                               HttpServletRequest request) {
        sendHit(request.getRemoteAddr(), request.getRequestURI());
        categoryService.deleteCategory(catId);
    }

    @PatchMapping("/{catId}")
    public CategoryDto updateCategory(@Valid @RequestBody CategoryDto category,
                                      @PathVariable long catId,
                                      HttpServletRequest request) {
        sendHit(request.getRemoteAddr(), request.getRequestURI());
        return categoryService.updateCategory(category, catId);
    }

    private void sendHit(String ip, String uri) {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("main-service")
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        statsClient.hit(hitDto);
    }
}