package fr.hesias.dmd.application.port.out;

import fr.hesias.dmd.domain.model.Report;

public interface ReportRepository {

    Report save(Report report);
}
