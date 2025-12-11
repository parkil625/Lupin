-- V4__insert_missing_doctors.sql (필수 필드만 포함 버전)

-- 1. Users 테이블 데이터 복구 (current_points, status 등 제거함)
-- 비밀번호는 모두 '1'
INSERT IGNORE INTO users (id, user_id, password, name, role, height, weight, gender, birth_date, department) VALUES
(21, 'doctor01', '$2a$10$c2TQaJnZaQLbMxjQ2GRs8OQr4aO7a6l8C0hKQ3wCGnA3lxmzz6vUi', '박카스', 'DOCTOR', 163.0, 52.0, '여성', '1988-07-22', '의료실'),
(22, 'doctor02', '$2a$10$c2TQaJnZaQLbMxjQ2GRs8OQr4aO7a6l8C0hKQ3wCGnA3lxmzz6vUi', '김준호', 'DOCTOR', 175.0, 70.0, '남성', '1985-05-10', '의료실'),
(23, 'doctor03', '$2a$10$c2TQaJnZaQLbMxjQ2GRs8OQr4aO7a6l8C0hKQ3wCGnA3lxmzz6vUi', '이정민', 'DOCTOR', 168.0, 58.0, '남성', '1982-11-18', '의료실'),
(24, 'doctor04', '$2a$10$c2TQaJnZaQLbMxjQ2GRs8OQr4aO7a6l8C0hKQ3wCGnA3lxmzz6vUi', '박지은', 'DOCTOR', 165.0, 55.0, '여성', '1990-03-25', '의료실');

-- 2. Doctor Profiles 테이블 데이터 복구 (문제없음, 그대로 사용)
INSERT IGNORE INTO doctor_profiles (user_id, specialty, license_number, medical_experience, phone, birth_date, gender, address, created_at) VALUES
(21, '내과', 'DOC-2024-001', 10, '010-9876-5432', '1988-07-22', '여성', '서울특별시 강남구', NOW()),
(22, '외과', 'DOC-2024-002', 12, '010-1234-5678', '1985-05-10', '남성', '서울특별시 서초구', NOW()),
(23, '신경외과', 'DOC-2024-003', 15, '010-2345-6789', '1982-11-18', '남성', '서울특별시 송파구', NOW()),
(24, '피부과', 'DOC-2024-004', 8, '010-3456-7890', '1990-03-25', '여성', '서울특별시 강동구', NOW());