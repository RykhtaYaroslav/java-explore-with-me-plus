package ru.practicum.main.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.user.dto.NewUserRequest;
import ru.practicum.main.user.dto.UserDto;
import ru.practicum.main.user.service.UserService;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@RequestMapping(value = "/admin/users")
@RestController
public class AdminUserController {
    private final UserService userService;
    private final StatsClient statsClient;

    @GetMapping
    public List<UserDto> getUsers(@RequestParam(required = false) List<Long> ids,
                                  @RequestParam(defaultValue = "0") int from,
                                  @RequestParam(defaultValue = "10") int size,
                                  HttpServletRequest request) {
        sendHit(request.getRemoteAddr(), request.getRequestURI());
        return userService.getUsers(ids, from, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody NewUserRequest user,
                              HttpServletRequest request) {
        sendHit(request.getRemoteAddr(), request.getRequestURI());
        return userService.createUser(user);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable long userId,
                           HttpServletRequest request) {
        sendHit(request.getRemoteAddr(), request.getRequestURI());
        userService.deleteUser(userId);
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
