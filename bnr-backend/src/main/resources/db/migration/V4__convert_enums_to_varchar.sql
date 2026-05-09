-- V4: Convert custom PostgreSQL ENUM columns to VARCHAR so Hibernate's
-- @Enumerated(EnumType.STRING) maps cleanly without a custom dialect.
-- Author: David NTAMAKEMWA

-- applications.status
ALTER TABLE applications
    ALTER COLUMN status TYPE VARCHAR(30) USING status::TEXT;

ALTER TABLE applications
    ALTER COLUMN status SET DEFAULT 'DRAFT';

-- users.role
ALTER TABLE users
    ALTER COLUMN role TYPE VARCHAR(20) USING role::TEXT;

-- Drop the now-unused ENUM types
DROP TYPE IF EXISTS application_status;
DROP TYPE IF EXISTS user_role;
