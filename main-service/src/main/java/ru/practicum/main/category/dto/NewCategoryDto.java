package ru.practicum.main.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewCategoryDto {
    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 1, max = 50, message = "Имя должно быть от {min} до {max} символов")
    private String name;
}
