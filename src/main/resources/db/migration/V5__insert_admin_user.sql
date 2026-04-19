INSERT INTO users (id, name, email, password, role, active)
VALUES (
    gen_random_uuid(),
    'Administrador',
    'admin@medical.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    TRUE
);
