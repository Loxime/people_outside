WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
    v_today DATE;

    PROCEDURE insert_sample(
        p_latitude IN BINARY_DOUBLE,
        p_longitude IN BINARY_DOUBLE,
        p_place_type IN VARCHAR2,
        p_crowd_level IN NUMBER,
        p_local_date IN DATE
    ) IS
        v_reported_at TIMESTAMP WITH TIME ZONE;
        v_existing NUMBER;
    BEGIN
        v_reported_at := FROM_TZ(
            CAST(p_local_date AS TIMESTAMP),
            'Europe/Paris'
        );

        SELECT COUNT(*)
          INTO v_existing
          FROM REPORTS
         WHERE LATITUDE = p_latitude
           AND LONGITUDE = p_longitude
           AND PLACE_TYPE = p_place_type
           AND CROWD_LEVEL = p_crowd_level
           AND REPORTED_AT = v_reported_at;

        IF v_existing = 0 THEN
            INSERT INTO REPORTS (
                LATITUDE,
                LONGITUDE,
                PLACE_TYPE,
                CROWD_LEVEL,
                REPORTED_AT
            ) VALUES (
                p_latitude,
                p_longitude,
                p_place_type,
                p_crowd_level,
                v_reported_at
            );
        END IF;
    END insert_sample;
BEGIN
    v_today := TRUNC(
        CAST(SYSTIMESTAMP AT TIME ZONE 'Europe/Paris' AS DATE)
    );

    -- Aujourd'hui : deux signalements proches.
    insert_sample(
        48.8570, 2.3520, 'PARK', 1, v_today + 12/24
    );

    insert_sample(
        48.8580, 2.3500, 'PLAYGROUND', 4, v_today + 13/24
    );

    -- Aujourd'hui : hors du rayon de 10 km.
    insert_sample(
        49.0000, 3.0000, 'SPORTS_FIELD', 3,
        v_today + 14/24
    );

    -- Hier : proche, mais hors de la journée courante.
    insert_sample(
        48.8567, 2.3523, 'PICNIC_AREA', 5,
        v_today - 1 + 12/24
    );
END;
/

COMMIT;

SELECT COUNT(*) AS TOTAL_REPORTS FROM REPORTS;

EXIT SUCCESS
