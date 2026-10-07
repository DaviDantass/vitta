-- Development-only demo accounts. Password for all accounts: password
-- These IDs are intentionally high to avoid colliding with normal local records.
INSERT INTO users (id, name, email, password, profile) VALUES
    (9001, 'Demo Doctor', 'doctor.demo@example.test', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'DOCTOR'),
    (9002, 'Demo Patient', 'patient.demo@example.test', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'PATIENT'),
    (9003, 'Demo Receptionist', 'receptionist.demo@example.test', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'RECEPTIONIST');

INSERT INTO doctors (id, name, email, phone, medical_registration, specialty) VALUES
    (9001, 'Demo Doctor', 'doctor.demo@example.test', '11999990001', '900001', 'CARDIOLOGY');

INSERT INTO patients (id, name, email, phone, cpf) VALUES
    (9002, 'Demo Patient', 'patient.demo@example.test', '11999990002', '900.001.900-02');

INSERT INTO appointments (id, doctor_id, patient_id, date_time) VALUES
    (9001, 9001, 9002, '2030-01-15 09:00:00');
