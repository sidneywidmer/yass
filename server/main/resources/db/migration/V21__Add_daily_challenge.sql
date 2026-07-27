ALTER TABLE game
    ADD COLUMN finished_state json;

ALTER TABLE game
    ALTER COLUMN seed TYPE BIGINT;

CREATE TABLE daily_challenge
(
    id           SERIAL PRIMARY KEY,
    uuid         VARCHAR(37)                 NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    day          DATE                        NOT NULL UNIQUE,
    seed         BIGINT                      NOT NULL,
    forced_decks json                        NOT NULL DEFAULT '[]'::json
);
