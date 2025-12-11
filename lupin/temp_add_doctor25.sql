INSERT INTO users (id, user_id, password, name, role, height, weight, gender, birth_date, department) VALUES
(25, 'doctor_dermatology', '$2a$10$c2TQaJnZaQLbMxjQ2GRs8OQr4aO7a6l8C0hKQ3wCGnA3lxmzz6vUi', '최지은', 'DOCTOR', 168.0, 58.0, '여성', '1990-03-25', '의료실');

INSERT INTO doctor_profiles (user_id, specialty, license_number, medical_experience, phone, birth_date, gender, address, created_at) VALUES
(25, '피부과', 'DOC-2024-025', 8, '010-4567-8901', '1990-03-25', '여성', '서울특별시 강동구', NOW());
