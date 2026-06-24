-- Migration: Add transaction_hash column and unique constraint to user_transactions
-- This enables duplicate detection when re-uploading bank statements.

-- Add the hash column (nullable at first, will be populated by the application)
ALTER TABLE user_transactions
    ADD COLUMN IF NOT EXISTS transaction_hash VARCHAR(64);

-- Add unique constraint on (user_id, transaction_hash) to prevent duplicates
-- Using DO block to handle case where constraint already exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'uq_transaction_hash'
          AND table_name = 'user_transactions'
    ) THEN
        ALTER TABLE user_transactions
            ADD CONSTRAINT uq_transaction_hash UNIQUE (user_id, transaction_hash);
    END IF;
END $$;

-- Create index for faster lookups by hash
CREATE INDEX IF NOT EXISTS idx_tx_user_hash ON user_transactions (user_id, transaction_hash);
