CREATE TABLE message
(
    id         SERIAL PRIMARY KEY,
    message    TEXT                        NOT NULL,
    meta       json                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
