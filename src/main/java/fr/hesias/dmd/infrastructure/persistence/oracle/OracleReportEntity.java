package fr.hesias.dmd.infrastructure.persistence.oracle;

import fr.hesias.dmd.domain.model.PlaceType;
import fr.hesias.dmd.domain.model.Report;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "REPORTS", schema = "PO_APP")
public class OracleReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(
            name = "LATITUDE",
            nullable = false,
            columnDefinition = "BINARY_DOUBLE"
    )
    private double latitude;

    @Column(
            name = "LONGITUDE",
            nullable = false,
            columnDefinition = "BINARY_DOUBLE"
    )
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "PLACE_TYPE", nullable = false, length = 32)
    private PlaceType placeType;

    @Column(
            name = "CROWD_LEVEL",
            nullable = false,
            columnDefinition = "NUMBER(1)"
    )
    private int crowdLevel;

    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    @Column(
            name = "REPORTED_AT",
            nullable = false,
            columnDefinition = "TIMESTAMP(6) WITH TIME ZONE"
    )
    private Instant reportedAt;

    protected OracleReportEntity() {
        // Required by JPA.
    }

    OracleReportEntity(Report report) {
        this.latitude = report.latitude();
        this.longitude = report.longitude();
        this.placeType = report.placeType();
        this.crowdLevel = report.crowdLevel();
        this.reportedAt = report.timestamp();
    }

    Report toDomain() {
        return new Report(
                id,
                latitude,
                longitude,
                placeType,
                crowdLevel,
                reportedAt
        );
    }
}
