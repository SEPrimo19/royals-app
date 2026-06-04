-- =============================================================================
-- Royals: The Kingdom Builders — Compassion participant tracking
--
-- Adds three columns to public.users:
--   is_compassion        BOOLEAN — is this member a Compassion-sponsored youth?
--   compassion_number    TEXT    — their Compassion ID (format PH867-XXXX)
--   emergency_contact    TEXT    — optional phone number for emergencies
--
-- A CHECK constraint enforces that Compassion participants have a valid
-- numbered ID matching ^PH867-\d{4}$ (prefix locked to PH867-, suffix is
-- the 4-digit member number the user enters at signup). The constraint
-- skips non-Compassion rows entirely so existing data is unaffected.
--
-- emergency_contact is intentionally NOT validated by regex here — PH phone
-- numbers vary (landline / mobile / international with +63) and a strict
-- regex would reject legitimate values. Client-side keyboardType=Phone
-- provides the right input experience; trust the user for v1.
--
-- RLS implications: no new policies needed. Existing user-row policies
-- already gate access — leaders see members in their group via group_id
-- match, admin/pastor see all. The new columns piggyback on that.
--
-- Safe to re-run.
-- =============================================================================

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS is_compassion BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS compassion_number TEXT;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS emergency_contact TEXT;

-- Drop and re-add the constraint so re-runs pick up any tweaks to the regex.
ALTER TABLE users
  DROP CONSTRAINT IF EXISTS users_compassion_number_valid;

ALTER TABLE users
  ADD CONSTRAINT users_compassion_number_valid
  CHECK (
    is_compassion = FALSE
    OR (compassion_number IS NOT NULL AND compassion_number ~ '^PH867-\d{4}$')
  );

-- Index on is_compassion for the upcoming admin compliance report filter
-- ("show me all Compassion participants"). Tiny table → index cost is trivial.
CREATE INDEX IF NOT EXISTS idx_users_is_compassion
  ON users (is_compassion) WHERE is_compassion = TRUE;
