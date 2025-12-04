INSERT INTO users (id, password, role)
VALUES (
    'admin',
    '$2a$10$t60PbsNbjztZ4k/fznGxperEe7wKkZV5rJf18CDYSSsje4XeUtj3K',  -- password: admin123
    'ADMIN'
)
ON CONFLICT (id) DO NOTHING;
