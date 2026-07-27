package ru.practicum.main.request.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Projection DTO used to encapsulate the total count of participation requests associated with a specific event.
 * <p>
 * Primarily used in JPQL aggregate queries with a {@code GROUP BY} clause to retrieve participation request counts
 * for multiple events in a single database round-trip, preventing the N+1 query problem.
 * </p>
 *
 * @param eventId the unique identifier of the target event
 * @param count   the total number of participation requests for the specified event and request status
 */
@Entity
@Table(name = "requests", indexes = {})
public record ConfirmedRequestsCount(Long eventId, Long count) {

    @Id
    private static Long id;

    //потом это все убери Сахар
}
// DTO for extracting confirmed requests from RequestRepository for eventServiceImpl