-- ============================================================
-- V2__add_enterprise_columns.sql
-- ============================================================
--
-- Description:
-- Adds enterprise-grade columns to the urls table.
--
-- Features:
-- • updated_at
-- • last_accessed_at
-- • is_active
-- • version (Optimistic Locking)
-- • Performance indexes
--
-- ============================================================

-- ------------------------------------------------------------
-- Add updated_at
-- ------------------------------------------------------------
ALTER TABLE urls
ADD COLUMN updated_at TIMESTAMP;

UPDATE urls
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE urls
ALTER COLUMN updated_at SET NOT NULL;

-- ------------------------------------------------------------
-- Add last_accessed_at
-- ------------------------------------------------------------
ALTER TABLE urls
ADD COLUMN last_accessed_at TIMESTAMP;

-- ------------------------------------------------------------
-- Add is_active
-- ------------------------------------------------------------
ALTER TABLE urls
ADD COLUMN is_active BOOLEAN DEFAULT TRUE;

UPDATE urls
SET is_active = TRUE
WHERE is_active IS NULL;

ALTER TABLE urls
ALTER COLUMN is_active SET NOT NULL;

-- ------------------------------------------------------------
-- Add optimistic locking version
-- ------------------------------------------------------------
ALTER TABLE urls
ADD COLUMN version BIGINT DEFAULT 0;

UPDATE urls
SET version = 0
WHERE version IS NULL;

ALTER TABLE urls
ALTER COLUMN version SET NOT NULL;

-- ============================================================
-- Additional Performance Indexes
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_urls_created_at
ON urls(created_at);

CREATE INDEX IF NOT EXISTS idx_urls_expires_at
ON urls(expires_at);

CREATE INDEX IF NOT EXISTS idx_urls_is_active
ON urls(is_active);

CREATE INDEX IF NOT EXISTS idx_urls_last_accessed_at
ON urls(last_accessed_at);