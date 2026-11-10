CREATE TABLE game_rooms
(
    id                   UUID PRIMARY KEY,
    game_id              UUID           NOT NULL REFERENCES games (id),
    owner_sub            VARCHAR(255)   NOT NULL,
    user_id              UUID,
    status               VARCHAR(16)    NOT NULL,
    billing_status       VARCHAR(16)    NOT NULL,
    end_reason           VARCHAR(16),
    container_id         VARCHAR(80),
    worker_ip            VARCHAR(45),
    webrtc_port          INT,
    rate_snapshot        NUMERIC(15, 2) NOT NULL,
    min_block_snapshot   INT            NOT NULL,
    max_duration_seconds INT            NOT NULL,
    billed_minutes       INT,
    billed_amount        NUMERIC(15, 2),
    debit_transaction_id UUID,
    created_at           TIMESTAMPTZ    NOT NULL,
    started_at           TIMESTAMPTZ,
    last_healthy_at      TIMESTAMPTZ,
    ended_at             TIMESTAMPTZ,
    version              BIGINT         NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_game_rooms_one_live_per_owner
    ON game_rooms (owner_sub)
    WHERE status IN ('STARTING', 'ACTIVE', 'STOPPING');

CREATE INDEX idx_game_rooms_status ON game_rooms (status);

CREATE INDEX idx_game_rooms_debit_pending
    ON game_rooms (billing_status)
    WHERE billing_status = 'DEBIT_PENDING';
