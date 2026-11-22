package fr.hesias.dmd.application;

import fr.hesias.dmd.application.port.out.ReportRepository;
import fr.hesias.dmd.domain.model.PlaceType;
import fr.hesias.dmd.domain.model.Report;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class CreateReportUseCase {

    private final ReportRepository repository;
    private final Clock clock;

    public CreateReportUseCase(
            ReportRepository repository,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Report create(
            double latitude,
            double longitude,
            PlaceType placeType,
            int crowdLevel
    ) {
        Report report = new Report(
                null,
                latitude,
                longitude,
                placeType,
                crowdLevel,
                clock.instant().truncatedTo(ChronoUnit.MICROS)
        );

        return repository.save(report);
    }
}
