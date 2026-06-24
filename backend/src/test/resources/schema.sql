-- H2-compatible schema for integration tests
DROP TABLE IF EXISTS ai_chat_history CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS spending_goals CASCADE;
DROP TABLE IF EXISTS user_transactions CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS app_users CASCADE;

CREATE TABLE app_users (
    uuid        VARCHAR(36)   PRIMARY KEY,
    username    VARCHAR(50)   NOT NULL UNIQUE,
    password    VARCHAR(255)  NOT NULL,
    email       VARCHAR(100)  NOT NULL UNIQUE,
    token       VARCHAR(255),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
    id          INT           AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)   NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO categories (name, description) VALUES
    ('FOOD',          'Groceries, restaurants, food delivery, snacks'),
    ('SHOPPING',      'In-store retail purchases, clothing, electronics'),
    ('WEB_SHOPPING',  'Online marketplace purchases'),
    ('SUBSCRIPTION',  'Recurring subscriptions'),
    ('TRANSPORT',     'Fuel, public transport, flights, cabs'),
    ('HEALTH',        'Medical, pharmacy, hospital, insurance'),
    ('UTILITIES',     'Electricity, water, gas, mobile, internet bills'),
    ('INCOME',        'Salary, interest, refunds'),
    ('TRANSFER',      'UPI / NEFT / IMPS transfers'),
    ('INVESTMENT',    'Stocks, mutual funds, FD'),
    ('EDUCATION',     'Courses, tuition, books'),
    ('RENT',          'House / office rent'),
    ('ENTERTAINMENT', 'Movies, events, gaming'),
    ('OTHER',         'Uncategorised');

CREATE TABLE user_transactions (
    id               VARCHAR(36)     PRIMARY KEY,
    user_id          VARCHAR(36)     NOT NULL,
    transaction_date DATE            NOT NULL,
    amount           DECIMAL(15, 2)  NOT NULL CHECK (amount > 0),
    type             VARCHAR(10)     NOT NULL CHECK (type IN ('DEBIT', 'CREDIT')),
    description      VARCHAR(500)    NOT NULL,
    category         VARCHAR(50)     NOT NULL DEFAULT 'OTHER',
    sent_to          VARCHAR(200),
    original_detail  VARCHAR(1000),
    transaction_hash VARCHAR(64),
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) REFERENCES app_users(uuid) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_category FOREIGN KEY (category) REFERENCES categories(name) ON DELETE SET DEFAULT,
    CONSTRAINT uq_transaction_hash UNIQUE (user_id, transaction_hash)
);

CREATE TABLE spending_goals (
    id              VARCHAR(36)     PRIMARY KEY,
    user_id         VARCHAR(36)     NOT NULL,
    category        VARCHAR(50)     NOT NULL,
    target_amount   DECIMAL(15, 2)  NOT NULL CHECK (target_amount > 0),
    period          VARCHAR(20)     NOT NULL CHECK (period IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    start_date      DATE            NOT NULL,
    end_date        DATE,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES app_users(uuid) ON DELETE CASCADE,
    CONSTRAINT fk_goal_category FOREIGN KEY (category) REFERENCES categories(name) ON DELETE CASCADE
);

CREATE TABLE notifications (
    id              VARCHAR(36)     PRIMARY KEY,
    user_id         VARCHAR(36)     NOT NULL,
    type            VARCHAR(30)     NOT NULL CHECK (type IN ('GOAL_WARNING', 'GOAL_EXCEEDED', 'BUDGET_TIP', 'SPENDING_INSIGHT', 'SYSTEM')),
    title           VARCHAR(200)    NOT NULL,
    message         VARCHAR(2000)   NOT NULL,
    read            BOOLEAN         NOT NULL DEFAULT FALSE,
    reference_id    VARCHAR(36),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES app_users(uuid) ON DELETE CASCADE
);

CREATE TABLE ai_chat_history (
    id          VARCHAR(36)     PRIMARY KEY,
    user_id     VARCHAR(36)     NOT NULL,
    role        VARCHAR(20)     NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content     VARCHAR(5000)   NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_chat_user FOREIGN KEY (user_id) REFERENCES app_users(uuid) ON DELETE CASCADE
);
