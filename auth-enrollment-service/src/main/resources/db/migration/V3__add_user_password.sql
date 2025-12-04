-- Add password column to users for credentialed login
ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255) NOT NULL DEFAULT '';

-- Optional: ensure existing seeded users have a non-null default; the service will backfill on first login.
