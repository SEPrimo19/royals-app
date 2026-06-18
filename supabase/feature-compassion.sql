
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS is_compassion BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS compassion_number TEXT;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS emergency_contact TEXT;

ALTER TABLE users
  DROP CONSTRAINT IF EXISTS users_compassion_number_valid;

ALTER TABLE users
  ADD CONSTRAINT users_compassion_number_valid
  CHECK (
    is_compassion = FALSE
    OR (compassion_number IS NOT NULL AND compassion_number ~ '^PH867-\d{4}$')
  );

CREATE INDEX IF NOT EXISTS idx_users_is_compassion
  ON users (is_compassion) WHERE is_compassion = TRUE;
