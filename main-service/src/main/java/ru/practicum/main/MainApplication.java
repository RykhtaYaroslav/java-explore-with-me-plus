package ru.practicum.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum.main", "ru.practicum.stats.client"})
public class MainApplication {
    public static void main(String[] args) {
        // Запускем приложение и сохраняем контекст в переменную
        ConfigurableApplicationContext context = SpringApplication.run(MainApplication.class, args);

        StatsClient statsClient = context.getBean(StatsClient.class);

        EndpointHitDto dto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/test/events/1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.now()).build();

        // Вызываем метод сохранения статистик
        statsClient.hit(dto);

        // Вызываем метод получения статистики
        Object stats = statsClient.getStats(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                List.of("/test/events/1"),
                true
        );

    }
}
