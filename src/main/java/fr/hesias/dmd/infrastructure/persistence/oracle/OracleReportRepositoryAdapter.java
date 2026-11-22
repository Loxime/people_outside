package fr.hesias.dmd.infrastructure.persistence.oracle;

import fr.hesias.dmd.application.port.out.ReportRepository;
import fr.hesias.dmd.application.port.out.ReportSearchPort;
import fr.hesias.dmd.domain.model.Report;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Repository
public class OracleReportRepositoryAdapter
        implements ReportRepository, ReportSearchPort {

    private final SpringDataReportRepository repository;

    public OracleReportRepositoryAdapter(
            SpringDataReportRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Report save(Report report) {
        Objects.requireNonNull(report, "report");

        if (report.id() != null) {
            throw new IllegalArgumentException(
                    "A new report must not have an ID"
            );
        }

        OracleReportEntity entity =
                new OracleReportEntity(report);

        return repository.save(entity).toDomain();
    }

    @Override
    public List<Report> findByTimestampRange(
            Instant startInclusive,
            Instant endExclusive
    ) {
        return repository.findByTimestampRange(
                        startInclusive,
                        endExclusive
                )
                .stream()
                .map(OracleReportEntity::toDomain)
                .toList();
    }
}
