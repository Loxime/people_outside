package fr.hesias.dmd.application;

import fr.hesias.dmd.application.port.out.ReportSearchPort;
import fr.hesias.dmd.domain.model.Report;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

public final class SearchReportsUseCase {

    private static final ZoneId PARIS_ZONE =
            ZoneId.of("Europe/Paris");

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final ReportSearchPort repository;
    private final Clock clock;

    public SearchReportsUseCase(
            ReportSearchPort repository,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public List<Report> search(
            double latitude,
            double longitude,
            double radiusKm
    ) {
        validateCoordinate(latitude, -90, 90, "Latitude");
        validateCoordinate(longitude, -180, 180, "Longitude");

        if (!Double.isFinite(radiusKm)
                || radiusKm <= 0
                || radiusKm > 100) {
            throw new IllegalArgumentException(
                    "Radius must be greater than 0 and at most 100 km"
            );
        }

        LocalDate today = clock.instant()
                .atZone(PARIS_ZONE)
                .toLocalDate();

        Instant start = today
                .atStartOfDay(PARIS_ZONE)
                .toInstant();

        Instant end = today.plusDays(1)
                .atStartOfDay(PARIS_ZONE)
                .toInstant();

        return repository.findByTimestampRange(start, end)
                .stream()
                .filter(report ->
                        distanceKm(
                                latitude,
                                longitude,
                                report.latitude(),
                                report.longitude()
                        ) <= radiusKm
                )
                .toList();
    }

    private static void validateCoordinate(
            double value,
            double min,
            double max,
            String name
    ) {
        if (!Double.isFinite(value)
                || value < min
                || value > max) {
            throw new IllegalArgumentException(
                    name + " must be between " + min + " and " + max
            );
        }
    }

    private static double distanceKm(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        double deltaLat = lat2 - lat1;
        double deltaLon =
                Math.toRadians(longitude2 - longitude1);

        double sinLat = Math.sin(deltaLat / 2);
        double sinLon = Math.sin(deltaLon / 2);

        double a = sinLat * sinLat
                + Math.cos(lat1) * Math.cos(lat2)
                * sinLon * sinLon;

        double safeA = Math.max(0, Math.min(1, a));

        return 2 * EARTH_RADIUS_KM
                * Math.asin(Math.sqrt(safeA));
    }
}
