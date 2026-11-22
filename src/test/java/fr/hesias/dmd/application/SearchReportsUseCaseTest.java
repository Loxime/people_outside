package fr.hesias.dmd.application;

import fr.hesias.dmd.application.port.out.ReportSearchPort;
import fr.hesias.dmd.domain.model.PlaceType;
import fr.hesias.dmd.domain.model.Report;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

class SearchReportsUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-22T12:00:00Z"),
            ZoneOffset.UTC
    );

    private static Report report(
            long id,
            double latitude,
            double longitude
    ) {
        return new Report(
                id,
                latitude,
                longitude,
                PlaceType.PARK,
                2,
                CLOCK.instant()
        );
    }

    @Test
    void shouldReturnOnlyNearbyReports() {
        Report first = report(1, 48.8566, 2.3522);
        Report second = report(2, 48.8580, 2.3500);
        Report far = report(3, 49.0, 3.0);

        RecordingPort port = new RecordingPort(
                List.of(first, second, far)
        );

        List<Report> results =
                new SearchReportsUseCase(port, CLOCK)
                        .search(48.8566, 2.3522, 1);

        assertEquals(List.of(first, second), results);
        assertEquals(1, port.calls);
    }

    @Test
    void shouldReturnEmptyListWhenNoneIsNearby() {
        RecordingPort port = new RecordingPort(
                List.of(report(1, 49.0, 3.0))
        );

        List<Report> results =
                new SearchReportsUseCase(port, CLOCK)
                        .search(48.8566, 2.3522, 1);

        assertTrue(results.isEmpty());
    }

    @Test
    void shouldReturnEveryMatchingReportWithoutLimit() {
        List<Report> reports = LongStream.rangeClosed(1, 12)
                .mapToObj(id -> report(id, 48.8566, 2.3522))
                .toList();

        RecordingPort port = new RecordingPort(reports);

        List<Report> results =
                new SearchReportsUseCase(port, CLOCK)
                        .search(48.8566, 2.3522, 1);

        assertEquals(reports, results);
    }

    @Test
    void shouldIncludeReportExactlyAtRadiusBoundary() {
        Report boundary = report(1, 0, 0.5);
        RecordingPort port = new RecordingPort(List.of(boundary));

        double radiusKm =
                Math.toRadians(0.5) * 6371.0088;

        List<Report> results =
                new SearchReportsUseCase(port, CLOCK)
                        .search(0, 0, radiusKm);

        assertEquals(List.of(boundary), results);
    }

    @Test
    void shouldHandleInternationalDateLine() {
        Report nearby = report(1, 0, -179.9);
        RecordingPort port = new RecordingPort(List.of(nearby));

        List<Report> results =
                new SearchReportsUseCase(port, CLOCK)
                        .search(0, 179.9, 25);

        assertEquals(List.of(nearby), results);
    }

    @Test
    void shouldAcceptBoundaryCoordinates() {
        Report boundary = report(1, -90, -180);
        RecordingPort port = new RecordingPort(List.of(boundary));

        List<Report> results =
                new SearchReportsUseCase(port, CLOCK)
                        .search(-90, -180, 1);

        assertEquals(List.of(boundary), results);
    }

    @Test
    void shouldRejectInvalidLatitudesWithoutQuery() {
        RecordingPort port = new RecordingPort(List.of());
        SearchReportsUseCase useCase =
                new SearchReportsUseCase(port, CLOCK);

        for (double latitude : new double[]{
                -90.01, 90.01,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        }) {
            assertThrows(IllegalArgumentException.class, () ->
                    useCase.search(latitude, 2, 10)
            );
        }

        assertEquals(0, port.calls);
    }

    @Test
    void shouldRejectInvalidLongitudesWithoutQuery() {
        RecordingPort port = new RecordingPort(List.of());
        SearchReportsUseCase useCase =
                new SearchReportsUseCase(port, CLOCK);

        for (double longitude : new double[]{
                -180.01, 180.01,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        }) {
            assertThrows(IllegalArgumentException.class, () ->
                    useCase.search(48, longitude, 10)
            );
        }

        assertEquals(0, port.calls);
    }

    @Test
    void shouldRejectInvalidRadiusWithoutQuery() {
        RecordingPort port = new RecordingPort(List.of());
        SearchReportsUseCase useCase =
                new SearchReportsUseCase(port, CLOCK);

        for (double radius : new double[]{
                0, -1, 100.01,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        }) {
            assertThrows(IllegalArgumentException.class, () ->
                    useCase.search(48, 2, radius)
            );
        }

        assertEquals(0, port.calls);
    }

    @Test
    void shouldAcceptMaximumRadius() {
        Report nearby = report(1, 0, 0.5);
        RecordingPort port = new RecordingPort(List.of(nearby));

        List<Report> results =
                new SearchReportsUseCase(port, CLOCK)
                        .search(0, 0, 100);

        assertEquals(List.of(nearby), results);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-11-22T12:00:00Z, 2026-11-21T23:00:00Z, 2026-11-22T23:00:00Z",
            "2026-03-29T12:00:00Z, 2026-03-28T23:00:00Z, 2026-03-29T22:00:00Z",
            "2026-10-25T12:00:00Z, 2026-10-24T22:00:00Z, 2026-10-25T23:00:00Z"
    })
    void shouldUseParisDayBoundaries(
            String now,
            String expectedStart,
            String expectedEnd
    ) {
        RecordingPort port = new RecordingPort(List.of());

        Clock clock = Clock.fixed(
                Instant.parse(now),
                ZoneOffset.UTC
        );

        new SearchReportsUseCase(port, clock)
                .search(48.8566, 2.3522, 10);

        assertAll(
                () -> assertEquals(1, port.calls),
                () -> assertEquals(
                        Instant.parse(expectedStart),
                        port.startInclusive
                ),
                () -> assertEquals(
                        Instant.parse(expectedEnd),
                        port.endExclusive
                )
        );
    }

    private static class RecordingPort
            implements ReportSearchPort {

        private final List<Report> reports;
        private int calls;
        private Instant startInclusive;
        private Instant endExclusive;

        private RecordingPort(List<Report> reports) {
            this.reports = List.copyOf(reports);
        }

        @Override
        public List<Report> findByTimestampRange(
                Instant startInclusive,
                Instant endExclusive
        ) {
            calls++;
            this.startInclusive = startInclusive;
            this.endExclusive = endExclusive;
            return reports;
        }
    }
}
