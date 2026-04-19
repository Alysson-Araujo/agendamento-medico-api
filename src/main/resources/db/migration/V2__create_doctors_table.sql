CREATE TABLE doctors (
    id        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id   UUID        NOT NULL UNIQUE REFERENCES users(id),
    crm       VARCHAR(20) NOT NULL UNIQUE,
    specialty VARCHAR(50) NOT NULL CHECK (specialty IN (
                  'CARDIOLOGY','DERMATOLOGY','ORTHOPEDICS',
                  'NEUROLOGY','PEDIATRICS','GENERAL_PRACTICE'
              )),
    phone     VARCHAR(20) NOT NULL,
    active    BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_doctors_specialty ON doctors(specialty);
