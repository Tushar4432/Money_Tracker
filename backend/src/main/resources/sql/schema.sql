-- ============================================================
-- Money Tracker — Database Schema (PostgreSQL)
-- One-time setup script. Run manually via:
--   psql -U postgres -d money_tracker -f schema.sql
-- ============================================================

DROP TABLE IF EXISTS file_uploads          CASCADE;
DROP TABLE IF EXISTS spending_goals        CASCADE;
DROP TABLE IF EXISTS user_transactions     CASCADE;
DROP TABLE IF EXISTS categories            CASCADE;
DROP TABLE IF EXISTS app_users             CASCADE;

CREATE TABLE app_users (
    uuid        VARCHAR(36)   PRIMARY KEY,
    username    VARCHAR(50)   NOT NULL UNIQUE,
    password    VARCHAR(255)  NOT NULL,
    email       VARCHAR(100)  NOT NULL UNIQUE,
    token       VARCHAR(255),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_app_users_username ON app_users (username);
CREATE INDEX idx_app_users_email    ON app_users (email);

CREATE TABLE categories (
    id          SERIAL        PRIMARY KEY,
    name        VARCHAR(50)   NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

INSERT INTO categories (name, description) VALUES
    ('FOOD',          'Groceries, restaurants, food delivery, snacks'),
    ('SHOPPING',      'In-store retail purchases, clothing, electronics'),
    ('WEB_SHOPPING',  'Online marketplace purchases (Amazon, Flipkart…)'),
    ('SUBSCRIPTION',  'Recurring subscriptions (Netflix, Spotify, etc.)'),
    ('TRANSPORT',     'Fuel, public transport, flights, cabs, IRCTC'),
    ('HEALTH',        'Medical, pharmacy, hospital, insurance'),
    ('UTILITIES',     'Electricity, water, gas, mobile, internet bills'),
    ('INCOME',        'Salary, interest, refunds, any money received'),
    ('TRANSFER',      'UPI / NEFT / IMPS transfers between accounts'),
    ('INVESTMENT',    'Stocks, mutual funds, FD, Zerodha, etc.'),
    ('EDUCATION',     'Courses, tuition, books, exam fees'),
    ('RENT',          'House / office rent payments'),
    ('ENTERTAINMENT', 'Movies, events, gaming, hobbies'),
    ('OTHER',         'Uncategorised transactions');

CREATE TABLE user_transactions (
    id               VARCHAR(36)     PRIMARY KEY,
    user_id          VARCHAR(36)     NOT NULL,
    transaction_date DATE            NOT NULL,
    amount           NUMERIC(15, 2)  NOT NULL CHECK (amount > 0),
    type             VARCHAR(10)     NOT NULL CHECK (type IN ('DEBIT', 'CREDIT')),
    description      VARCHAR(500)    NOT NULL,
    category         VARCHAR(50)     NOT NULL DEFAULT 'OTHER',
    sent_to          VARCHAR(200),
    original_detail  TEXT,
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transaction_user
        FOREIGN KEY (user_id) REFERENCES app_users (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_category
        FOREIGN KEY (category) REFERENCES categories (name) ON DELETE SET DEFAULT
);

CREATE INDEX idx_tx_user_id       ON user_transactions (user_id);
CREATE INDEX idx_tx_user_date     ON user_transactions (user_id, transaction_date DESC);
CREATE INDEX idx_tx_user_category ON user_transactions (user_id, category);
CREATE INDEX idx_tx_user_type     ON user_transactions (user_id, type);
CREATE INDEX idx_tx_date          ON user_transactions (transaction_date DESC);
CREATE INDEX idx_tx_category      ON user_transactions (category);
CREATE INDEX idx_tx_sent_to       ON user_transactions (sent_to);
CREATE INDEX idx_tx_user_cat_date ON user_transactions (user_id, category, transaction_date DESC);

CREATE TABLE spending_goals (
    id              VARCHAR(36)     PRIMARY KEY,
    user_id         VARCHAR(36)     NOT NULL,
    category        VARCHAR(50)     NOT NULL,
    target_amount   NUMERIC(15, 2)  NOT NULL CHECK (target_amount > 0),
    period          VARCHAR(20)     NOT NULL CHECK (period IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    start_date      DATE            NOT NULL,
    end_date        DATE,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_goal_user
        FOREIGN KEY (user_id) REFERENCES app_users (uuid) ON DELETE CASCADE,
    CONSTRAINT fk_goal_category
        FOREIGN KEY (category) REFERENCES categories (name) ON DELETE CASCADE
);

CREATE INDEX idx_goals_user_id  ON spending_goals (user_id);
CREATE INDEX idx_goals_user_cat ON spending_goals (user_id, category);
CREATE INDEX idx_goals_active   ON spending_goals (user_id, active);

CREATE TABLE file_uploads (
    id              VARCHAR(36)     PRIMARY KEY,
    user_id         VARCHAR(36)     NOT NULL,
    file_name       VARCHAR(255)    NOT NULL,
    file_type       VARCHAR(10)     NOT NULL CHECK (file_type IN ('CSV', 'XLSX', 'XLS')),
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                                    CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    total_rows      INT             DEFAULT 0,
    processed_rows  INT             DEFAULT 0,
    failed_rows     INT             DEFAULT 0,
    error_message   TEXT,
    uploaded_at     TIMESTAMP       NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMP,

    CONSTRAINT fk_upload_user
        FOREIGN KEY (user_id) REFERENCES app_users (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_uploads_user_id     ON file_uploads (user_id);
CREATE INDEX idx_uploads_status      ON file_uploads (status);
CREATE INDEX idx_uploads_uploaded_at ON file_uploads (uploaded_at DESC);

CREATE TABLE notifications (
    id              VARCHAR(36)     PRIMARY KEY,
    user_id         VARCHAR(36)     NOT NULL,
    type            VARCHAR(30)     NOT NULL CHECK (type IN ('GOAL_WARNING', 'GOAL_EXCEEDED', 'BUDGET_TIP', 'SPENDING_INSIGHT', 'SYSTEM')),
    title           VARCHAR(200)    NOT NULL,
    message         TEXT            NOT NULL,
    read            BOOLEAN         NOT NULL DEFAULT FALSE,
    reference_id    VARCHAR(36),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id) REFERENCES app_users (uuid) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_id   ON notifications (user_id);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, read);
CREATE INDEX idx_notifications_type      ON notifications (type);
CREATE INDEX idx_notifications_created   ON notifications (created_at DESC);
