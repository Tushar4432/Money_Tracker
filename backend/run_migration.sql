-- ============================================================
-- Money Tracker — Migration Script
-- Run this against your PostgreSQL database to add duplicate
-- detection for transactions.
--
--   psql -U postgres -d money_tracker -f run_migration.sql
-- ============================================================

-- 1. Add the hash column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'user_transactions'
          AND column_name = 'transaction_hash'
    ) THEN
        ALTER TABLE user_transactions
            ADD COLUMN transaction_hash VARCHAR(64);
        RAISE NOTICE 'Added transaction_hash column';
    ELSE
        RAISE NOTICE 'transaction_hash column already exists';
    END IF;
END $$;

-- 2. Add unique constraint if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'uq_transaction_hash'
          AND table_name = 'user_transactions'
    ) THEN
        ALTER TABLE user_transactions
            ADD CONSTRAINT uq_transaction_hash UNIQUE (user_id, transaction_hash);
        RAISE NOTICE 'Added uq_transaction_hash constraint';
    ELSE
        RAISE NOTICE 'uq_transaction_hash constraint already exists';
    END IF;
END $$;

-- 3. Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_tx_user_hash ON user_transactions (user_id, transaction_hash);

-- 4. Verify
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'user_transactions'
ORDER BY ordinal_position;
