-- Atendimentos de balcão podem não ter um cliente cadastrado.
ALTER TABLE appointments ALTER COLUMN client_id DROP NOT NULL;
