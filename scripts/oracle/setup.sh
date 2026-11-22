#!/usr/bin/env bash
set -euo pipefail

if [[ ! "${APP_DB_PASSWORD:-}" =~ ^P0_[a-f0-9]{40}$ ]]; then
    echo "Invalid or missing APP_DB_PASSWORD" >&2
    exit 1
fi

sqlplus -L -s "/ as sysdba" <<SQL
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET FEEDBACK OFF

ALTER SESSION SET CONTAINER = FREEPDB1;


DECLARE
    tablespace_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO tablespace_count
    FROM DBA_TABLESPACES
    WHERE TABLESPACE_NAME = 'PO_DATA';

    IF tablespace_count = 0 THEN
        EXECUTE IMMEDIATE q'[
            CREATE TABLESPACE PO_DATA
            DATAFILE '/opt/oracle/oradata/FREE/FREEPDB1/po_data01.dbf'
            SIZE 32M
            AUTOEXTEND ON NEXT 16M
            MAXSIZE 128M
        ]';
    END IF;
END;
/


DECLARE
    user_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO user_count
    FROM DBA_USERS
    WHERE USERNAME = 'PO_APP';

    IF user_count = 0 THEN
        EXECUTE IMMEDIATE
            'CREATE USER PO_APP IDENTIFIED BY "$APP_DB_PASSWORD"
             DEFAULT TABLESPACE PO_DATA
             QUOTA 100M ON PO_DATA';
    END IF;
END;
/

GRANT CREATE SESSION TO PO_APP;

DECLARE
    table_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO table_count
    FROM DBA_TABLES
    WHERE OWNER = 'PO_APP'
      AND TABLE_NAME = 'REPORTS';

    IF table_count = 0 THEN
        EXECUTE IMMEDIATE q'[
            CREATE TABLE PO_APP.REPORTS (
                ID NUMBER(19)
                    GENERATED ALWAYS AS IDENTITY
                    PRIMARY KEY,
                LATITUDE BINARY_DOUBLE NOT NULL,
                LONGITUDE BINARY_DOUBLE NOT NULL,
                PLACE_TYPE VARCHAR2(32 CHAR) NOT NULL,
                CROWD_LEVEL NUMBER(1) NOT NULL,
                REPORTED_AT TIMESTAMP(6)
                    WITH TIME ZONE NOT NULL,

                CONSTRAINT CK_REPORTS_LATITUDE
                    CHECK (LATITUDE BETWEEN -90 AND 90),

                CONSTRAINT CK_REPORTS_LONGITUDE
                    CHECK (LONGITUDE BETWEEN -180 AND 180),

                CONSTRAINT CK_REPORTS_CROWD
                    CHECK (CROWD_LEVEL BETWEEN 1 AND 5),

                CONSTRAINT CK_REPORTS_PLACE
                    CHECK (PLACE_TYPE IN (
                        'PARK', 'BEACH', 'PLAYGROUND',
                        'SPORTS_FIELD', 'PICNIC_AREA'
                    ))
            )
        ]';
    END IF;
END;
/

EXIT SUCCESS
SQL
