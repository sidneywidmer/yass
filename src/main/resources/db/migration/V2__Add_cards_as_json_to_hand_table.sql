ALTER TABLE hand
    ADD COLUMN north json NOT NULL,
    ADD COLUMN east json NOT NULL,
    ADD COLUMN south json NOT NULL,
    ADD COLUMN west json NOT NULL;

