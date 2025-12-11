-- V4__insert_doctors.sql
-- 진료과별 의사 데이터 추가
-- 비밀번호는 모두 '1' (doctor01과 동일한 해시)

-- 1. Users 테이블에 의사 추가
INSERT IGNORE INTO users (id, user_id, password, name, role, height, weight, gender, birth_date, department) VALUES
(22, 'doctor02', '$2a$10$c2TQaJnZaQLbMxjQ2GRs8OQr4aO7a6l8C0hKQ3wCGnA3lxmzz6vUi', '김민수', 'DOCTOR', 175.0, 70.0, '남성', '1985-05-10', '의료실'),
(23, 'doctor03', '$2a$10$c2TQaJnZaQLbMxjQ2GRs8OQr4aO7a6l8C0hKQ3wCGnA3lxmzz6vUi', '이준호', 'DOCTOR', 178.0, 75.0, '남성', '1982-11-18', '의료실'),
(24, 'doctor04', '$2a$10$c2TQaJnZaQLbMxjQ2GRs8OQr4aO7a6l8C0hKQ3wCGnA3lxmzz6vUi', '박서연', 'DOCTOR', 165.0, 55.0, '여성', '1988-07-22', '의료실'),
(25, 'doctor05', '$2a$10$c2TQaJnZaQLbMxjQ2GRs8OQr4aO7a6l8C0hKQ3wCGnA3lxmzz6vUi', '최지은', 'DOCTOR', 168.0, 58.0, '여성', '1990-03-25', '의료실');

-- 2. Doctor Profiles 테이블에 상세 정보 추가
INSERT IGNORE INTO doctor_profiles (user_id, specialty, license_number, medical_experience, phone, birth_date, gender, address, created_at) VALUES
(22, '내과', 'DOC-2024-022', 12, '010-1234-5678', '1985-05-10', '남성', '서울특별시 서초구', NOW()),
(23, '외과', 'DOC-2024-023', 15, '010-2345-6789', '1982-11-18', '남성', '서울특별시 송파구', NOW()),
(24, '신경정신과', 'DOC-2024-024', 10, '010-3456-7890', '1988-07-22', '여성', '서울특별시 강남구', NOW()),
(25, '피부과', 'DOC-2024-025', 8, '010-4567-8901', '1990-03-25', '여성', '서울특별시 강동구', NOW());
