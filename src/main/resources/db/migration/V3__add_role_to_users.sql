ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'USER';

CREATE INDEX idx_users_role ON users(role);