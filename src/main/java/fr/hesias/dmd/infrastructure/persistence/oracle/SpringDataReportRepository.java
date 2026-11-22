package fr.hesias.dmd.infrastructure.persistence.oracle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SpringDataReportRepository
        extends JpaRepository<OracleReportEntity, Long> {

    @Query("""
            select report
            from OracleReportEntity report
            where report.reportedAt >= :startInclusive
              and report.reportedAt < :endExclusive
            """)
    List<OracleReportEntity> findByTimestampRange(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );
}
