CREATE TABLE books (
                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                       title TEXT NOT NULL,
                       author TEXT NOT NULL,
                       isbn TEXT,
                       publication_year INTEGER,
                       genre TEXT,
                       reading_status TEXT NOT NULL DEFAULT 'NOT_STARTED',
                       rating INTEGER,
                       notes TEXT,
                       user_id INTEGER NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                       CHECK (reading_status IN ('NOT_STARTED', 'READING', 'COMPLETED')),
                       CHECK (rating IS NULL OR (rating >= 1 AND rating <= 5))
);

CREATE INDEX idx_books_user_id ON books(user_id);
CREATE INDEX idx_books_reading_status ON books(reading_status);