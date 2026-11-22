package fr.hesias.dmd.application;

import fr.hesias.dmd.application.port.out.ReportRepository;
import fr.hesias.dmd.domain.model.PlaceType;
import fr.hesias.dmd.domain.model.Report;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class CreateReportUseCaseTest {

    private static final Instant NOW =
            Instant.parse("2026-11-22T12:00:00Z");

    private static final Clock CLOCK =
            Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldPersistValidReportOnce() {
        RecordingRepository repository = new RecordingRepository();
        CreateReportUseCase useCase =
                new CreateReportUseCase(repository, CLOCK);

        Report result = useCase.create(
                48.8566, 2.3522, PlaceType.PARK, 2
        );

        assertAll(
                () -> assertEquals(1, repository.saveCalls),
                () -> assertNotNull(repository.received),
                () -> assertNull(repository.received.id()),
                () -> assertEquals(NOW, repository.received.timestamp()),
                () -> assertEquals(42L, result.id()),
                () -> assertEquals(48.8566, result.latitude()),
                () -> assertEquals(PlaceType.PARK, result.placeType()),
                () -> assertEquals(2, result.crowdLevel()),
                () -> assertEquals(NOW, result.timestamp())
        );
    }

    @Test
    void shouldUseOracleTimestampPrecision() {
        Instant now =
                Instant.parse("2026-11-22T17:16:46.376058522Z");
        Instant expected =
                Instant.parse("2026-11-22T17:16:46.376058Z");

        RecordingRepository repository = new RecordingRepository();
        CreateReportUseCase useCase = new CreateReportUseCase(
                repository,
                Clock.fixed(now, ZoneOffset.UTC)
        );

        Report saved = useCase.create(
                48.8566, 2.3522, PlaceType.PARK, 2
        );

        assertEquals(expected, repository.received.timestamp());
        assertEquals(expected, saved.timestamp());
        assertEquals(1, repository.saveCalls);
    }

    @Test
    void shouldNotPersistInvalidCoordinates() {
        RecordingRepository repository = new RecordingRepository();
        CreateReportUseCase useCase =
                new CreateReportUseCase(repository, CLOCK);

        assertThrows(IllegalArgumentException.class, () ->
                useCase.create(
                        Double.NaN, 2.3522, PlaceType.PARK, 2
                )
        );

        assertEquals(0, repository.saveCalls);
    }

    @Test
    void shouldNotPersistInvalidCrowdLevel() {
        RecordingRepository repository = new RecordingRepository();
        CreateReportUseCase useCase =
                new CreateReportUseCase(repository, CLOCK);

        assertThrows(IllegalArgumentException.class, () ->
                useCase.create(
                        48.8566, 2.3522, PlaceType.PARK, 6
                )
        );

        assertEquals(0, repository.saveCalls);
    }

    @Test
    void shouldNotPersistMissingPlaceType() {
        RecordingRepository repository = new RecordingRepository();
        CreateReportUseCase useCase =
                new CreateReportUseCase(repository, CLOCK);

        assertThrows(IllegalArgumentException.class, () ->
                useCase.create(48.8566, 2.3522, null, 2)
        );

        assertEquals(0, repository.saveCalls);
    }

    private static class RecordingRepository
            implements ReportRepository {

        private int saveCalls;
        private Report received;

        @Override
        public Report save(Report report) {
            saveCalls++;
            received = report;

            return new Report(
                    42L,
                    report.latitude(),
                    report.longitude(),
                    report.placeType(),
                    report.crowdLevel(),
                    report.timestamp()
            );
        }
    }
}
