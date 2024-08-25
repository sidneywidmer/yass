ALTER TABLE hand
    DROP COLUMN IF EXISTS north_stoeck,
    DROP COLUMN IF EXISTS east_stoeck,
    DROP COLUMN IF EXISTS south_stoeck,
    DROP COLUMN IF EXISTS west_stoeck;