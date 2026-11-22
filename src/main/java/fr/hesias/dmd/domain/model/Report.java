package fr.hesias.dmd.domain.model;

import java.time.Instant;

public record Report(
        Long id,
        double latitude,
        double longitude,
        PlaceType placeType,
        int crowdLevel,
        Instant timestamp
) {

    public Report {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException(
                    "Report ID must be positive"
            );
        }

        validateCoordinate(latitude, -90, 90, "Latitude");
        validateCoordinate(longitude, -180, 180, "Longitude");

        if (placeType == null) {
            throw new IllegalArgumentException(
                    "Place type is required"
            );
        }

        if (crowdLevel < 1 || crowdLevel > 5) {
            throw new IllegalArgumentException(
                    "Crowd level must be between 1 and 5"
            );
        }

        if (timestamp == null) {
            throw new IllegalArgumentException(
                    "Timestamp is required"
            );
        }
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
}
