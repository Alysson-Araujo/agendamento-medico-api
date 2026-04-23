INSERT INTO users (id, name, email, password, role, active)
VALUES (
    gen_random_uuid(),
    'Administrador',
    'admin@medical.com',
    '$2a$10$QqlAmiVPp5QtF4LOSdvNLeG4i.X1S2gaD1F7XknlSFJua3xThSmUy',
    'ADMIN',
    TRUE
);
