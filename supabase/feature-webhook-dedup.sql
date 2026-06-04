-- =============================================================================
-- GRACE — Webhook idempotency table
--
-- Supabase Database Webhooks have at-least-once delivery: if an Edge Function
-- returns non-2xx (or times out), the webhook retries with exponential backoff.
-- Without an idempotency guard, every retry causes a full FCM/email fan-out.
--
-- This table is a tiny dedup ledger. Each Edge Function constructs a stable
-- dedup_key (e.g. "notify-event-created:{event_id}") and inserts a row at the
-- top of doWork. If the insert hits a UNIQUE violation, the function knows
-- the request was already processed and returns 200 OK without doing the
-- side-effects again.
--
-- No retention policy is enforced here — the table grows by ~1 row per
-- notification, which for a youth ministry is small forever. If size ever
-- matters, prune rows older than 7 days via a scheduled function.
--
-- Safe to re-run.
-- =============================================================================

CREATE TABLE IF NOT EXISTS webhook_dedup (
  dedup_key     TEXT PRIMARY KEY,
  processed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- RLS: service role only (functions use SUPABASE_SERVICE_ROLE_KEY, which
-- bypasses RLS). Regular users have no business reading or writing this.
ALTER TABLE webhook_dedup ENABLE ROW LEVEL SECURITY;
-- No policies = no access for authenticated users. Service role bypasses RLS.
