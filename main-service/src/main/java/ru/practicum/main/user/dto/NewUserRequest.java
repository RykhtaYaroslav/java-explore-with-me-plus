package ru.practicum.main.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewUserRequest {
    @Email(message = "Неверный формат почты")
    @NotBlank(message = "Почта не может быть пустой")
    private String email;

    @NotBlank(message = "Имя не может быть пустым")
    private String name;
}
