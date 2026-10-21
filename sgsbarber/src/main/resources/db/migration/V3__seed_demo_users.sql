INSERT INTO clients (name, phone, email, cpf, birth_date)
SELECT 'Cliente Demo', '(81) 90000-0000', 'cliente@sgsbarber.com', '00000000000', DATE '2000-01-01'
WHERE NOT EXISTS (SELECT 1 FROM clients WHERE email = 'cliente@sgsbarber.com');

INSERT INTO barbers (name, email, phone, commission_percent, active)
SELECT 'Administrador SGS', 'admin@sgsbarber.com', '(81) 90000-0000', 50, TRUE
WHERE NOT EXISTS (SELECT 1 FROM barbers WHERE email = 'admin@sgsbarber.com');

INSERT INTO users (role, name, email, phone, cpf, password_hash, client_id, active)
SELECT 'client', c.name, c.email, c.phone, c.cpf,
       '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', c.id, TRUE
FROM clients c
WHERE c.email = 'cliente@sgsbarber.com'
  AND NOT EXISTS (SELECT 1 FROM users WHERE email = 'cliente@sgsbarber.com');

INSERT INTO users (role, name, email, phone, cpf, password_hash, barber_id, active)
SELECT 'barber', b.name, b.email, b.phone,
       '00000000000',
       '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', b.id, TRUE
FROM barbers b
WHERE b.email = 'admin@sgsbarber.com'
  AND NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@sgsbarber.com');
