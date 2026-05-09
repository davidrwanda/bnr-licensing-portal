-- V3: Seed data — one user per role, two applications in different states.
-- Passwords are BCrypt-hashed. Plain-text values are in the README.
-- Author: David NTAMAKEMWA

-- Admin@12345
INSERT INTO users (id, email, password_hash, full_name, role) VALUES
    ('a0000000-0000-0000-0000-000000000001',
     'admin@bnr.rw',
     '$2a$12$m1L3VdIvlSpfPouc0uRv2.66wyl5Pt6vQxvxIKkg29mgs3NqyHSgO',
     'System Administrator',
     'ADMIN');

-- Review@12345
INSERT INTO users (id, email, password_hash, full_name, role) VALUES
    ('a0000000-0000-0000-0000-000000000002',
     'reviewer@bnr.rw',
     '$2a$12$d1ZaGjkDfuH0X30Ue9rKyuImq40TDnV3zmluyjhuBpeJF2AjP44OS',
     'Jean Pierre HABIMANA',
     'REVIEWER');

-- Approve@12345
INSERT INTO users (id, email, password_hash, full_name, role) VALUES
    ('a0000000-0000-0000-0000-000000000003',
     'approver@bnr.rw',
     '$2a$12$/bVcBmFl3fIev9NNzAM7Duv.IFcweAmXZ6SSEWHmBFQA3N9BZBQ0e',
     'Marie Claire UWIMANA',
     'APPROVER');

-- Apply@12345
INSERT INTO users (id, email, password_hash, full_name, role) VALUES
    ('a0000000-0000-0000-0000-000000000004',
     'applicant@example.rw',
     '$2a$12$wE/9jf5b8QE7bUeQ9a2jYepPGPJNIs6AuXQAU.Y15XfHi03D3uoRW',
     'Eric NKURUNZIZA',
     'APPLICANT');

-- Application A: SUBMITTED — ready for reviewer assignment
INSERT INTO applications (id, applicant_id, institution_name, institution_type,
                          contact_address, business_description, status, submitted_at)
VALUES ('b0000000-0000-0000-0000-000000000001',
        'a0000000-0000-0000-0000-000000000004',
        'Kigali Community Savings Bank',
        'Commercial Bank',
        'KG 123 St, Kigali',
        'A community-focused savings and lending institution serving small businesses in Kigali.',
        'SUBMITTED',
        NOW() - INTERVAL '3 days');

-- Application B: REVIEW_COMPLETE — reviewed by reviewer@bnr.rw, awaiting final decision.
-- Log in as approver@bnr.rw to test separation-of-duties enforcement.
INSERT INTO applications (id, applicant_id, reviewer_id, institution_name, institution_type,
                          contact_address, business_description, status, reviewer_notes, submitted_at)
VALUES ('b0000000-0000-0000-0000-000000000002',
        'a0000000-0000-0000-0000-000000000004',
        'a0000000-0000-0000-0000-000000000002',
        'Rwanda Digital Finance Ltd',
        'Microfinance Institution',
        'KG 456 Ave, Kigali',
        'A digital-first microfinance institution providing mobile-based lending products.',
        'REVIEW_COMPLETE',
        'All submitted documents are in order. Capital adequacy ratios meet the minimum threshold. Recommend approval.',
        NOW() - INTERVAL '7 days');

-- Seed audit log entries for both applications
INSERT INTO audit_logs (application_id, actor_id, actor_email, action, previous_status, new_status, metadata)
VALUES
    ('b0000000-0000-0000-0000-000000000001',
     'a0000000-0000-0000-0000-000000000004',
     'applicant@example.rw',
     'APPLICATION_SUBMITTED',
     'DRAFT', 'SUBMITTED',
     '{"note":"Initial submission"}'),

    ('b0000000-0000-0000-0000-000000000002',
     'a0000000-0000-0000-0000-000000000004',
     'applicant@example.rw',
     'APPLICATION_SUBMITTED',
     'DRAFT', 'SUBMITTED',
     '{"note":"Initial submission"}'),

    ('b0000000-0000-0000-0000-000000000002',
     'a0000000-0000-0000-0000-000000000001',
     'admin@bnr.rw',
     'REVIEWER_ASSIGNED',
     'SUBMITTED', 'UNDER_REVIEW',
     '{"assignedReviewer":"reviewer@bnr.rw"}'),

    ('b0000000-0000-0000-0000-000000000002',
     'a0000000-0000-0000-0000-000000000002',
     'reviewer@bnr.rw',
     'REVIEW_COMPLETED',
     'UNDER_REVIEW', 'REVIEW_COMPLETE',
     '{"notes":"Documents verified. Capital ratios meet threshold."}');
