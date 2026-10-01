-- doctor_id already has its NOT NULL foreign key from V2.
ALTER TABLE appointments ADD COLUMN patient_id BIGINT NULL;

-- The old form stored the patient's CPF as text. Link existing appointments
-- only to a matching registered patient; do not invent personal information.
UPDATE appointments a
JOIN patients p ON REPLACE(REPLACE(TRIM(a.patient), '.', ''), '-', '') = REPLACE(REPLACE(p.cpf, '.', ''), '-', '')
SET a.patient_id = p.id;

-- If a legacy CPF is not registered, this deliberately fails and preserves
-- the original patient column. Register matching patients before migrating.
ALTER TABLE appointments
    MODIFY COLUMN patient_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_appointments_patient_id FOREIGN KEY (patient_id) REFERENCES patients(id),
    DROP COLUMN patient;
