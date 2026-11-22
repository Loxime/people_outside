package fr.hesias.dmd.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReportTest {

    private static final Instant TIMESTAMP =
            Instant.parse("2026-11-22T12:00:00Z");

    private Report createReport(
            Long id,
            double latitude,
            double longitude,
            PlaceType placeType,
            int crowdLevel,
            Instant timestamp
    ) {
        return new Report(
                id,
                latitude,
                longitude,
                placeType,
                crowdLevel,
                timestamp
        );
    }

    @Test
    void shouldCreateValidReportBeforePersistence() {
        Report report = createReport(
                null, 48.8566, 2.3522,
                PlaceType.PARK, 2, TIMESTAMP
        );

        assertAll(
                () -> assertNull(report.id()),
                () -> assertEquals(48.8566, report.latitude()),
                () -> assertEquals(2.3522, report.longitude()),
                () -> assertEquals(PlaceType.PARK, report.placeType()),
                () -> assertEquals(2, report.crowdLevel()),
                () -> assertEquals(TIMESTAMP, report.timestamp())
        );
    }

    @Test
    void shouldAcceptPositiveId() {
        Report report = createReport(
                1L, 48, 2, PlaceType.BEACH, 3, TIMESTAMP
        );

        assertEquals(1L, report.id());
    }

    @Test
    void shouldRejectNonPositiveId() {
        for (long id : new long[]{0, -1}) {
            assertThrows(IllegalArgumentException.class, () ->
                    createReport(
                            id, 48, 2,
                            PlaceType.PARK, 2, TIMESTAMP
                    )
            );
        }
    }

    @Test
    void shouldAcceptLatitudeBoundaries() {
        for (double latitude : new double[]{-90, 90}) {
            assertDoesNotThrow(() ->
                    createReport(
                            null, latitude, 2,
                            PlaceType.PARK, 2, TIMESTAMP
                    )
            );
        }
    }

    @Test
    void shouldRejectInvalidLatitude() {
        double[] invalidValues = {
                -90.01,
                90.01,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        };

        for (double latitude : invalidValues) {
            assertThrows(IllegalArgumentException.class, () ->
                    createReport(
                            null, latitude, 2,
                            PlaceType.PARK, 2, TIMESTAMP
                    )
            );
        }
    }

    @Test
    void shouldAcceptLongitudeBoundaries() {
        for (double longitude : new double[]{-180, 180}) {
            assertDoesNotThrow(() ->
                    createReport(
                            null, 48, longitude,
                            PlaceType.PARK, 2, TIMESTAMP
                    )
            );
        }
    }

    @Test
    void shouldRejectInvalidLongitude() {
        double[] invalidValues = {
                -180.01,
                180.01,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        };

        for (double longitude : invalidValues) {
            assertThrows(IllegalArgumentException.class, () ->
                    createReport(
                            null, 48, longitude,
                            PlaceType.PARK, 2, TIMESTAMP
                    )
            );
        }
    }

    @Test
    void shouldAcceptCrowdLevelBoundaries() {
        for (int crowdLevel : new int[]{1, 5}) {
            assertDoesNotThrow(() ->
                    createReport(
                            null, 48, 2,
                            PlaceType.PARK, crowdLevel, TIMESTAMP
                    )
            );
        }
    }

    @Test
    void shouldRejectCrowdLevelOutsideRange() {
        for (int crowdLevel : new int[]{0, 6}) {
            assertThrows(IllegalArgumentException.class, () ->
                    createReport(
                            null, 48, 2,
                            PlaceType.PARK, crowdLevel, TIMESTAMP
                    )
            );
        }
    }

    @Test
    void shouldRejectMissingPlaceType() {
        assertThrows(IllegalArgumentException.class, () ->
                createReport(
                        null, 48, 2, null, 2, TIMESTAMP
                )
        );
    }

    @Test
    void shouldRejectMissingTimestamp() {
        assertThrows(IllegalArgumentException.class, () ->
                createReport(
                        null, 48, 2, PlaceType.PARK, 2, null
                )
        );
    }
}
