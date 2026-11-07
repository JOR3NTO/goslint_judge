-- =============================================================================
-- V2 — Equipos y miembros de equipo para contest-service
-- =============================================================================

CREATE TABLE IF NOT EXISTS teams (
    id            UUID                     PRIMARY KEY,
    contest_id    UUID                     NOT NULL REFERENCES contests (id) ON DELETE CASCADE,
    team_name     VARCHAR(50)              NOT NULL,
    score         INTEGER                  NOT NULL DEFAULT 0,
    penalty_time  BIGINT                   NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_teams_contest_name UNIQUE (contest_id, team_name),
    CONSTRAINT chk_teams_score CHECK (score >= 0 AND penalty_time >= 0)
);

CREATE INDEX IF NOT EXISTS idx_teams_scoreboard
    ON teams (contest_id, score DESC, penalty_time ASC);

CREATE TABLE IF NOT EXISTS team_members (
    id             UUID                     PRIMARY KEY,
    team_id        UUID                     NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    user_id        UUID                     NOT NULL,
    registered_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_team_members_team_user UNIQUE (team_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_team_members_user_id
    ON team_members (user_id);
