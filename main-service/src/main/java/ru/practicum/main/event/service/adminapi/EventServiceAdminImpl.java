package ru.practicum.main.event.service.adminapi;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventQueryParams;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceAdminImpl implements EventServiceAdmin {
    private final EventRepository eventRepository;
    private final EventStatsCollector eventStatsCollector;

    @Override
    public List<EventFullDto> getAllFullInfoWithParams(EventQueryParams params) {
        List<Event> events = getEventsWithParamsFromRepository(params);

        return eventStatsCollector.getFullDtoListWithStats(events);
    }

    private List<Event> getEventsWithParamsFromRepository(EventQueryParams params) {

        List<String> states = params.states().stream().map(Enum::name).toList();

        String sort = (params.sort() != null) ? params.sort().name() : null;

        return eventRepository.findAllWithParams(
                params.text(),
                params.categories(),
                params.paid(),
                params.rangeStart(),
                params.rangeEnd(),
                params.onlyAvailable(),
                sort,
                params.users(),
                states,
                params.from(),
                params.size());
    }
}
