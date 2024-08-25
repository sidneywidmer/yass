ALTER TABLE hand
    ADD north_weise json NOT NULL DEFAULT '[]'::json,
    ADD east_weise json NOT NULL DEFAULT '[]'::json,
    ADD south_weise json NOT NULL DEFAULT '[]'::json,
    ADD west_weise json NOT NULL DEFAULT '[]'::json,
    ADD north_stoeck boolean,
    ADD east_stoeck boolean,
    ADD south_stoeck boolean,
    ADD west_stoeck boolean;