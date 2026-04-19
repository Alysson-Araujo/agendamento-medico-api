CREATE TABLE patients (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL UNIQUE REFERENCES users(id),
    cpf        VARCHAR(14) NOT NULL UNIQUE,
    phone      VARCHAR(20) NOT NULL,
    birth_date DATE        NOT NULL,
    active     BOOLEAN     NOT NULL DEFAULT TRUE
);
