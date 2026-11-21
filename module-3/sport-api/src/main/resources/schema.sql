CREATE TABLE IF NOT EXISTS sports (
    id   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- Names are unique regardless of case: "Golf" and "golf" are the same sport
CREATE UNIQUE INDEX IF NOT EXISTS sports_name_key ON sports (LOWER(name));
