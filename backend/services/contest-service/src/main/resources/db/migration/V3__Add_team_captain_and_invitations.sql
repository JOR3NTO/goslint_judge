-- =============================================================================
-- V3 — Capitán de equipo e invitaciones
-- =============================================================================

ALTER TABLE teams ADD COLUMN IF NOT EXISTS captain_id UUID;

-- Los equipos existentes toman como capitán a su primer integrante.
UPDATE teams t
   SET captain_id = (SELECT m.user_id FROM team_members m
                      WHERE m.team_id = t.id ORDER BY m.registered_at LIMIT 1)
 WHERE captain_id IS NULL;

CREATE TABLE IF NOT EXISTS team_invitations (
    id               UUID                     PRIMARY KEY,
    team_id          UUID                     NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    invited_user_id  UUID                     NOT NULL,
    invited_email    VARCHAR(255)             NOT NULL,
    status           VARCHAR(20)              NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    responded_at     TIMESTAMP WITH TIME ZONE
);

-- Una sola invitación pendiente por equipo y usuario, también ante peticiones simultáneas.
CREATE UNIQUE INDEX IF NOT EXISTS uq_team_invitations_pending
    ON team_invitations (team_id, invited_user_id) WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_team_invitations_invited_user
    ON team_invitations (invited_user_id);
