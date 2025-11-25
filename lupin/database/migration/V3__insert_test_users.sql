-- ============================================
-- Test Users for Login
-- Date: 2025-11-25
-- Description:
--   - 일반 직원: user01 / 1
--   - 의사: doctor01 / 1
-- ============================================

-- BCrypt 인코딩된 비밀번호 "1"
-- $2a$10$N9qo8uLOickgx2ZMRZoMy.bIZEbP5kSNdGTfp4r4U/4IhPQ8lZlWO

-- 1. 일반 직원 (MEMBER)
INSERT INTO users (user_id, email, password, real_name, role, gender, birth_date, height, weight, current_points, monthly_points, monthly_likes, department, phone)
VALUES (
    'user01',
    'user01@company.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.bIZEbP5kSNdGTfp4r4U/4IhPQ8lZlWO',
    '김직원',
    'MEMBER',
    '남성',
    '1990-01-01',
    175.0,
    70.0,
    100,
    100,
    10,
    '개발팀',
    '010-1234-5678'
);

-- 2. 의사 직원 (DOCTOR)
INSERT INTO users (user_id, email, password, real_name, role, gender, birth_date, height, weight, current_points, monthly_points, monthly_likes, department, phone)
VALUES (
    'doctor01',
    'doctor01@company.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.bIZEbP5kSNdGTfp4r4U/4IhPQ8lZlWO',
    '박의사',
    'DOCTOR',
    '여성',
    '1985-05-15',
    165.0,
    55.0,
    50,
    50,
    5,
    '의료실',
    '010-9876-5432'
);

-- 3. 의사 프로필 생성 (doctor01용)
INSERT INTO doctor_profiles (user_id, specialty, license_number, medical_experience, phone, birth_date, gender, address, created_at)
SELECT
    id,
    '가정의학과',
    'DOC-2024-001',
    10,
    '010-9876-5432',
    '1985-05-15',
    '여성',
    '서울특별시 강남구',
    NOW()
FROM users
WHERE user_id = 'doctor01';

-- ============================================
-- 검증 쿼리
-- ============================================
-- SELECT * FROM users WHERE user_id IN ('user01', 'doctor01');
-- SELECT u.user_id, u.real_name, u.role, dp.specialty, dp.license_number
-- FROM users u
-- LEFT JOIN doctor_profiles dp ON dp.user_id = u.id
-- WHERE u.user_id IN ('user01', 'doctor01');
