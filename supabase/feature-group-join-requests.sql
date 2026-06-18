
CREATE TABLE IF NOT EXISTS group_join_requests (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  group_id      UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
  user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status        TEXT NOT NULL DEFAULT 'pending'
                  CHECK (status IN ('pending','approved','rejected','cancelled','expired')),
  decided_by    UUID REFERENCES users(id),
  decided_at    TIMESTAMPTZ,
  decided_note  TEXT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DROP INDEX IF EXISTS idx_gjr_one_pending_per_group_user;
CREATE UNIQUE INDEX idx_gjr_one_pending_per_group_user
  ON group_join_requests (group_id, user_id)
  WHERE status = 'pending';

CREATE INDEX IF NOT EXISTS idx_gjr_group_status   ON group_join_requests (group_id, status);
CREATE INDEX IF NOT EXISTS idx_gjr_user_status    ON group_join_requests (user_id, status);
CREATE INDEX IF NOT EXISTS idx_gjr_created        ON group_join_requests (created_at);


CREATE OR REPLACE FUNCTION public.guard_group_join_request()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_pending_count       INT;
  v_last_request_at     TIMESTAMPTZ;
  v_last_rejected_at    TIMESTAMPTZ;
BEGIN
  -- Only run guards for pending inserts (the path users actually trigger).
  IF NEW.status <> 'pending' THEN
    RETURN NEW;
  END IF;

  -- Cap: at most 3 simultaneously pending requests across all groups per user.
  SELECT count(*) INTO v_pending_count
    FROM group_join_requests
   WHERE user_id = NEW.user_id AND status = 'pending';
  IF v_pending_count >= 3 THEN
    RAISE EXCEPTION
      'You already have 3 pending requests. Cancel one before requesting another.'
      USING ERRCODE = 'check_violation';
  END IF;

  -- Rate-limit: max 1 request per (group, user) per 24h, regardless of status.
  -- Prevents spam-resubmit after a cancellation.
  SELECT max(created_at) INTO v_last_request_at
    FROM group_join_requests
   WHERE user_id = NEW.user_id AND group_id = NEW.group_id;
  IF v_last_request_at IS NOT NULL
     AND v_last_request_at > NOW() - INTERVAL '24 hours' THEN
    RAISE EXCEPTION
      'You can only request to join this cell once per 24 hours.'
      USING ERRCODE = 'check_violation';
  END IF;

  -- Cooldown: 7 days after a rejection before re-requesting the same cell.
  SELECT max(decided_at) INTO v_last_rejected_at
    FROM group_join_requests
   WHERE user_id = NEW.user_id
     AND group_id = NEW.group_id
     AND status = 'rejected';
  IF v_last_rejected_at IS NOT NULL
     AND v_last_rejected_at > NOW() - INTERVAL '7 days' THEN
    RAISE EXCEPTION
      'This cell recently declined your request. Try again after 7 days.'
      USING ERRCODE = 'check_violation';
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_guard_group_join_request ON group_join_requests;
CREATE TRIGGER trg_guard_group_join_request
  BEFORE INSERT ON group_join_requests
  FOR EACH ROW
  EXECUTE FUNCTION public.guard_group_join_request();


CREATE OR REPLACE FUNCTION public.handle_group_join_decision()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF OLD.status = 'pending' AND NEW.status = 'approved' THEN
    -- Single-cell invariant: remove from every other group first.
    DELETE FROM group_members
     WHERE user_id = NEW.user_id
       AND group_id <> NEW.group_id;

    -- Add to the new group (idempotent).
    INSERT INTO group_members (user_id, group_id)
    VALUES (NEW.user_id, NEW.group_id)
    ON CONFLICT (user_id, group_id) DO NOTHING;

    -- Mirror into legacy users.group_id pointer so existing screens that
    -- still read it (ProfileSetup, getMyLifeGroup fallback) stay consistent.
    UPDATE users
       SET group_id = NEW.group_id
     WHERE id = NEW.user_id;
  END IF;

  -- Audit stamp on every transition out of 'pending'. Doesn't overwrite if
  -- the caller already supplied decided_at (e.g. the auto-expire cron).
  IF OLD.status = 'pending' AND NEW.status <> 'pending' THEN
    NEW.decided_at := COALESCE(NEW.decided_at, NOW());
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_handle_group_join_decision ON group_join_requests;
CREATE TRIGGER trg_handle_group_join_decision
  BEFORE UPDATE ON group_join_requests
  FOR EACH ROW
  WHEN (OLD.status IS DISTINCT FROM NEW.status)
  EXECUTE FUNCTION public.handle_group_join_decision();


ALTER TABLE group_join_requests ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS gjr_select_self_or_leader ON group_join_requests;
DROP POLICY IF EXISTS gjr_insert_self ON group_join_requests;
DROP POLICY IF EXISTS gjr_update_self_cancel ON group_join_requests;
DROP POLICY IF EXISTS gjr_update_leader_decide ON group_join_requests;

CREATE POLICY gjr_select_self_or_leader ON group_join_requests
  FOR SELECT USING (
    auth.uid() = user_id
    OR auth.uid() = (SELECT leader_id FROM groups WHERE id = group_id)
    OR EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin','council')
    )
  );

CREATE POLICY gjr_insert_self ON group_join_requests
  FOR INSERT WITH CHECK (
    auth.uid() = user_id
  );

CREATE POLICY gjr_update_self_cancel ON group_join_requests
  FOR UPDATE USING (
    auth.uid() = user_id AND status = 'pending'
  ) WITH CHECK (
    auth.uid() = user_id AND status IN ('cancelled')
  );

CREATE POLICY gjr_update_leader_decide ON group_join_requests
  FOR UPDATE USING (
    status = 'pending'
    AND (
      auth.uid() = (SELECT leader_id FROM groups WHERE id = group_id)
      OR EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid()
          AND role IN ('youth_president','pastor','admin')
      )
    )
  ) WITH CHECK (
    status IN ('approved','rejected')
  );


CREATE OR REPLACE FUNCTION public.list_browsable_groups()
RETURNS TABLE (
  id              UUID,
  name            TEXT,
  description     TEXT,
  leader_id       UUID,
  leader_name     TEXT,
  member_count    BIGINT,
  is_my_group     BOOLEAN,
  my_pending_id   UUID
)
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
  WITH counts AS (
    SELECT group_id, count(*)::bigint AS c FROM group_members GROUP BY group_id
  )
  SELECT
    g.id,
    g.name,
    g.description,
    g.leader_id,
    COALESCE(l.name, '—'),
    COALESCE(c.c, 0),
    EXISTS (
      SELECT 1 FROM group_members gm
       WHERE gm.group_id = g.id AND gm.user_id = auth.uid()
    ) AS is_my_group,
    (
      SELECT r.id FROM group_join_requests r
       WHERE r.group_id = g.id
         AND r.user_id  = auth.uid()
         AND r.status   = 'pending'
       LIMIT 1
    ) AS my_pending_id
  FROM groups g
  LEFT JOIN users l   ON l.id = g.leader_id
  LEFT JOIN counts c  ON c.group_id = g.id
  ORDER BY g.name ASC;
$$;

GRANT EXECUTE ON FUNCTION public.list_browsable_groups() TO authenticated;


CREATE OR REPLACE FUNCTION public.list_incoming_join_requests()
RETURNS TABLE (
  id                 UUID,
  group_id           UUID,
  group_name         TEXT,
  user_id            UUID,
  user_name          TEXT,
  user_email         TEXT,
  user_avatar_url    TEXT,
  user_is_compassion BOOLEAN,
  user_current_group TEXT,
  created_at         TIMESTAMPTZ
)
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
  SELECT
    r.id,
    r.group_id,
    g.name AS group_name,
    r.user_id,
    u.name,
    u.email,
    u.avatar_url,
    COALESCE(u.is_compassion, FALSE),
    cg.name AS user_current_group,
    r.created_at
  FROM group_join_requests r
  JOIN groups g    ON g.id = r.group_id
  JOIN users  u    ON u.id = r.user_id
  LEFT JOIN groups cg ON cg.id = u.group_id
  WHERE r.status = 'pending'
    AND (
      -- The cell's own leader sees their inbox …
      auth.uid() = g.leader_id
      -- … plus senior leaders see everything.
      OR EXISTS (
        SELECT 1 FROM users me
        WHERE me.id = auth.uid()
          AND me.role IN ('youth_president','pastor','admin')
      )
    )
  ORDER BY r.created_at ASC;
$$;

GRANT EXECUTE ON FUNCTION public.list_incoming_join_requests() TO authenticated;
