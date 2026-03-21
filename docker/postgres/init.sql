-- CareStream PostgreSQL initialization
-- Creates all service databases on first startup

CREATE DATABASE auth_db;
CREATE DATABASE patient_db;
CREATE DATABASE audit_db;
CREATE DATABASE security_db;

-- Grant all privileges to the carestream user
GRANT ALL PRIVILEGES ON DATABASE auth_db TO carestream;
GRANT ALL PRIVILEGES ON DATABASE patient_db TO carestream;
GRANT ALL PRIVILEGES ON DATABASE audit_db TO carestream;
GRANT ALL PRIVILEGES ON DATABASE security_db TO carestream;
