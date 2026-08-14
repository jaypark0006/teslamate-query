-- Run as superuser / TeslaMate DB owner against the TeslaMate database.
-- Creates a least-privilege reader for teslamate-query.

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'teslamate_query') THEN
    CREATE ROLE teslamate_query LOGIN PASSWORD 'change-me';
  END IF;
END$$;

GRANT CONNECT ON DATABASE teslamate TO teslamate_query;
GRANT USAGE ON SCHEMA public TO teslamate_query;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO teslamate_query;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO teslamate_query;

-- Explicitly no access to private tokens if schema exists
REVOKE ALL ON SCHEMA private FROM teslamate_query;
