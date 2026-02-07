CREATE TABLE IF NOT EXISTS role (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    role_id BIGINT NOT NULL,
    refresh_token TEXT,
    email VARCHAR(255),
    google_id VARCHAR(255) UNIQUE,
    provider VARCHAR(50),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES role (id)
);

CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

INSERT INTO role(type)
VALUES ('USER'), ('ADMIN')
ON CONFLICT (type) DO NOTHING;
