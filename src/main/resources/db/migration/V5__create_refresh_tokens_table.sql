CREATE TABLE refresh_tokens (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                token TEXT NOT NULL UNIQUE,
                                user_id INTEGER NOT NULL,
                                expiry_date TIMESTAMP NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                revoked INTEGER DEFAULT 0,
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expiry_date ON refresh_tokens(expiry_date);