ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS client_name VARCHAR(120);
