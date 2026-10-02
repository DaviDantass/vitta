-- Doctors now share the primary key generated for their user account.
-- Existing doctors must already reference the intended users by ID.
ALTER TABLE appointments DROP FOREIGN KEY fk_appointments_doctor_id;

ALTER TABLE doctors MODIFY id BIGINT NOT NULL;

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_doctor_id FOREIGN KEY (doctor_id) REFERENCES doctors(id);

ALTER TABLE doctors
    ADD CONSTRAINT fk_doctors_user_id FOREIGN KEY (id) REFERENCES users(id);
