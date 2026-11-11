CREATE TABLE games
(
    id                UUID PRIMARY KEY,
    slug              VARCHAR(64)    NOT NULL UNIQUE,
    title             VARCHAR(128)   NOT NULL,
    docker_image      VARCHAR(255)   NOT NULL,
    rate_per_minute   NUMERIC(15, 2) NOT NULL CHECK (rate_per_minute > 0),
    min_block_minutes INT            NOT NULL DEFAULT 1 CHECK (min_block_minutes >= 1),
    supports_keyboard BOOLEAN        NOT NULL DEFAULT TRUE,
    supports_mouse    BOOLEAN        NOT NULL DEFAULT TRUE,
    supports_gamepad  BOOLEAN        NOT NULL DEFAULT FALSE,
    is_active         BOOLEAN        NOT NULL DEFAULT TRUE
);
