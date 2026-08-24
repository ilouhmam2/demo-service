CREATE TABLE items (
    id          BIGSERIAL                NOT NULL PRIMARY KEY,
    name        VARCHAR(255)             NOT NULL,
    description VARCHAR(1000),
    created_at  TIMESTAMP WITH TIME ZONE,
    updated_at  TIMESTAMP WITH TIME ZONE
);
