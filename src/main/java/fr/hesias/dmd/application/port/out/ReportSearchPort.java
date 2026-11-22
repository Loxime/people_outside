package fr.hesias.dmd.application.port.out;

import fr.hesias.dmd.domain.model.Report;

import java.time.Instant;
import java.util.List;

public interface ReportSearchPort {

    List<Report> findByTimestampRange(
            Instant startInclusive,
            Instant endExclusive
    );
}
