package fr.hesias.dmd.infrastructure.persistence.oracle;

import fr.hesias.dmd.application.SearchReportsUseCase;
import fr.hesias.dmd.application.port.out.ReportRepository;
import fr.hesias.dmd.application.port.out.ReportSearchPort;
import fr.hesias.dmd.domain.model.PlaceType;
import fr.hesias.dmd.domain.model.Report;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
class OracleReportRepositoryIT {

    @Autowired
    private ReportRepository repository;

    @Autowired
    private ReportSearchPort searchPort;

    @Autowired
    private SpringDataReportRepository springDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldPersistAndReloadReportFromOracle() {
        Instant timestamp =
                Instant.parse("2026-11-22T16:30:00.123456Z");

        Report report = new Report(
                null,
                48.8566,
                2.3522,
                PlaceType.PARK,
                2,
                timestamp
        );

        Report saved = repository.save(report);

        assertNotNull(saved.id());
        assertTrue(saved.id() > 0);

        entityManager.flush();
        entityManager.clear();

        Report reloaded = springDataRepository
                .findById(saved.id())
                .orElseThrow()
                .toDomain();

        assertEquals(saved, reloaded);
        assertEquals(timestamp, reloaded.timestamp());
    }

    @Test
    void shouldPreserveDistinctInstantsAcrossDstTransition() {
        Instant first =
                Instant.parse("2026-10-25T00:30:00Z");

        Instant second =
                Instant.parse("2026-10-25T01:30:00Z");

        Report firstReport = repository.save(
                new Report(
                        null, 48.8566, 2.3522,
                        PlaceType.PARK, 2, first
                )
        );

        Report secondReport = repository.save(
                new Report(
                        null, 48.8566, 2.3522,
                        PlaceType.PARK, 3, second
                )
        );

        entityManager.flush();
        entityManager.clear();

        Instant firstStored = springDataRepository
                .findById(firstReport.id())
                .orElseThrow()
                .toDomain()
                .timestamp();

        Instant secondStored = springDataRepository
                .findById(secondReport.id())
                .orElseThrow()
                .toDomain()
                .timestamp();

        assertEquals(first, firstStored);
        assertEquals(second, secondStored);
        assertNotEquals(firstStored, secondStored);
    }

    @Test
    void shouldRespectInclusiveStartAndExclusiveEnd() {
        Instant start = Instant.parse("2035-11-22T00:00:00Z");
        Instant end = Instant.parse("2035-11-23T00:00:00Z");

        Report before = saveAt(start.minusNanos(1_000));
        Report atStart = saveAt(start);
        Report beforeEnd = saveAt(end.minusNanos(1_000));
        Report atEnd = saveAt(end);

        entityManager.flush();
        entityManager.clear();

        var ids = searchPort.findByTimestampRange(start, end)
                .stream()
                .map(Report::id)
                .toList();

        assertAll(
                () -> assertFalse(ids.contains(before.id())),
                () -> assertTrue(ids.contains(atStart.id())),
                () -> assertTrue(ids.contains(beforeEnd.id())),
                () -> assertFalse(ids.contains(atEnd.id()))
        );
    }

    @Test
    void shouldFindBothOccurrencesOfRepeatedParisHour() {
        Instant start = Instant.parse("2026-10-24T22:00:00Z");
        Instant end = Instant.parse("2026-10-25T23:00:00Z");

        Report first = saveAt(
                Instant.parse("2026-10-25T00:30:00Z")
        );
        Report second = saveAt(
                Instant.parse("2026-10-25T01:30:00Z")
        );

        entityManager.flush();
        entityManager.clear();

        var ids = searchPort.findByTimestampRange(start, end)
                .stream()
                .map(Report::id)
                .toList();

        assertAll(
                () -> assertTrue(ids.contains(first.id())),
                () -> assertTrue(ids.contains(second.id()))
        );
    }


    @Test
    void shouldRejectReportWithAssignedId() {
        Report report = new Report(
                9999L,
                48.8566,
                2.3522,
                PlaceType.PARK,
                2,
                Instant.parse("2035-11-22T12:00:00Z")
        );

        InvalidDataAccessApiUsageException exception = assertThrows(
                InvalidDataAccessApiUsageException.class,
                () -> repository.save(report)
        );

        assertInstanceOf(
                IllegalArgumentException.class,
                exception.getCause()
        );
        assertEquals(
                "A new report must not have an ID",
                exception.getCause().getMessage()
        );
    }

    @Test
    void shouldRejectNullReport() {
        assertThrows(
                NullPointerException.class,
                () -> repository.save(null)
        );
    }

    @Test
    void shouldPersistIndependentObservationsAtSamePosition() {
        Instant timestamp =
                Instant.parse("2035-11-20T12:00:00Z");

        Report first = saveAt(timestamp);
        Report second = saveAt(timestamp);

        assertNotEquals(first.id(), second.id());

        entityManager.flush();
        entityManager.clear();

        Report firstStored = springDataRepository
                .findById(first.id())
                .orElseThrow()
                .toDomain();

        Report secondStored = springDataRepository
                .findById(second.id())
                .orElseThrow()
                .toDomain();

        assertAll(
                () -> assertEquals(first, firstStored),
                () -> assertEquals(second, secondStored)
        );
    }

    @Test
    void shouldFilterDateAndDistanceAgainstRealOracle() {
        Instant today =
                Instant.parse("2035-11-22T12:00:00Z");

        Report nearby = saveAt(today);

        Report anotherNearby = repository.save(new Report(
                null,
                48.8580,
                2.3500,
                PlaceType.PLAYGROUND,
                4,
                today
        ));

        Report far = repository.save(new Report(
                null,
                49.0000,
                3.0000,
                PlaceType.PARK,
                2,
                today
        ));

        Report yesterday = saveAt(
                Instant.parse("2035-11-21T12:00:00Z")
        );

        entityManager.flush();
        entityManager.clear();

        SearchReportsUseCase search = new SearchReportsUseCase(
                searchPort,
                Clock.fixed(today, ZoneOffset.UTC)
        );

        var returnedIds = search
                .search(48.8566, 2.3522, 10)
                .stream()
                .map(Report::id)
                .toList();

        assertAll(
                () -> assertTrue(returnedIds.contains(nearby.id())),
                () -> assertTrue(
                        returnedIds.contains(anotherNearby.id())
                ),
                () -> assertFalse(returnedIds.contains(far.id())),
                () -> assertFalse(
                        returnedIds.contains(yesterday.id())
                )
        );
    }

    private Report saveAt(Instant timestamp) {
        return repository.save(new Report(
                null,
                48.8566,
                2.3522,
                PlaceType.PARK,
                2,
                timestamp
        ));
    }
}
