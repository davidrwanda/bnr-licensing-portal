-- V2: Restrict audit_logs to INSERT-only for the application user.
-- This prevents any UPDATE or DELETE through the application layer,
-- making the audit trail tamper-resistant at the database privilege level.
-- Author: David NTAMAKEMWA

-- Revoke UPDATE and DELETE from the application database user.
-- The actual username must match DB_USERNAME in the environment.
-- If running under a superuser for development, this is still correct;
-- the bnr_user role will have restricted grants in production.

REVOKE UPDATE, DELETE ON audit_logs FROM PUBLIC;

-- Ensure the sequence is still usable
GRANT USAGE ON SEQUENCE audit_logs_id_seq TO PUBLIC;
