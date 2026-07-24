package ru.practicum.main.user.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.user.dto.NewUserRequest;
import ru.practicum.main.user.dto.UserDto;
import ru.practicum.main.user.dto.UserMapper;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.util.List;
import java.util.Objects;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        if (ids == null) {
            return userRepository.getUsersWithoutIds(from, size).stream()
                    .map(mapper::toDto)
                    .toList();
        }
        //По тз непонятно будут ли передаваться id юзеров которых нет(хотя ответа 404 нет в спецификации)
        //Но лучше всего сделать проверку на null
        return userRepository.findAllById(ids).stream()
                .filter(Objects::nonNull)
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public UserDto createUser(NewUserRequest newUser) {
        if (userRepository.findCountUserFromSelectEmail(newUser.getEmail()) != 0) {
            throw new ConflictException("Юзер с данным email = " + newUser.getEmail() + " уже существует");
        }
        User user = mapper.toEntity(newUser);
        return mapper.toDto(userRepository.save(user));
    }

    @Override
    public void deleteUser(long userId) {
        userRepository.findById(userId).orElseThrow(()
                -> new NotFoundException("Юзер с id = " + userId + " не найден"));

        userRepository.deleteById(userId);
    }
}
