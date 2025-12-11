-- ============================================
-- Test Users for Login
-- Date: 2025-11-27
-- Description:
--   - 일반 직원: user01 / 1 (박선일)
--   - 의사: doctor01 / 1 (홍세민)
-- ============================================

-- BCrypt 인코딩된 비밀번호 "1"
-- $2a$10$N9qo8uLOickgx2ZMRZoMy.bIZEbP5kSNdGTfp4r4U/4IhPQ8lZlWO

-- 1. 일반 직원 (MEMBER) - 박선일
INSERT IGNORE INTO users (user_id, password, name, role, height, weight, gender, birth_date, department, avatar)
VALUES (
    'user01',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.bIZEbP5kSNdGTfp4r4U/4IhPQ8lZlWO',
    '박선일',
    'MEMBER',
    178.5,
    72.0,
    '남성',
    '1992-03-15',
    '개발팀',
    NULL
);

-- 2. 의사 (DOCTOR) - 홍세민 (내과)
INSERT IGNORE INTO users (user_id, password, name, role, height, weight, gender, birth_date, department, avatar)
VALUES (
    'doctor01',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.bIZEbP5kSNdGTfp4r4U/4IhPQ8lZlWO',
    '홍세민',
    'DOCTOR',
    163.0,
    52.0,
    '여성',
    '1988-07-22',
    '의료실',
    NULL
);

-- 3. 의사 (DOCTOR) - 김준호 (외과)
INSERT IGNORE INTO users (user_id, password, name, role, height, weight, gender, birth_date, department, avatar)
VALUES (
    'doctor02',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.bIZEbP5kSNdGTfp4r4U/4IhPQ8lZlWO',
    '김준호',
    'DOCTOR',
    175.0,
    70.0,
    '남성',
    '1985-05-10',
    '의료실',
    NULL
);

-- 4. 의사 (DOCTOR) - 이정민 (신경외과)
INSERT IGNORE INTO users (user_id, password, name, role, height, weight, gender, birth_date, department, avatar)
VALUES (
    'doctor03',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.bIZEbP5kSNdGTfp4r4U/4IhPQ8lZlWO',
    '이정민',
    'DOCTOR',
    168.0,
    58.0,
    '남성',
    '1982-11-18',
    '의료실',
    NULL
);

-- 5. 의사 (DOCTOR) - 박지은 (피부과)
INSERT IGNORE INTO users (user_id, password, name, role, height, weight, gender, birth_date, department, avatar)
VALUES (
    'doctor04',
    '$2a$10$N9qo8uLOickgx2ZMRZoMy.bIZEbP5kSNdGTfp4r4U/4IhPQ8lZlWO',
    '박지은',
    'DOCTOR',
    165.0,
    55.0,
    '여성',
    '1990-03-25',
    '의료실',
    NULL
);

-- 6. 의사 프로필 생성 (doctor01용 - 내과)
INSERT IGNORE INTO doctor_profiles (user_id, specialty, license_number, medical_experience, phone, birth_date, gender, address, created_at)
SELECT
    id,
    '내과',
    'DOC-2024-001',
    10,
    '010-9876-5432',
    '1988-07-22',
    '여성',
    '서울특별시 강남구',
    NOW()
FROM users
WHERE user_id = 'doctor01';

-- 7. 의사 프로필 생성 (doctor02용 - 외과)
INSERT IGNORE INTO doctor_profiles (user_id, specialty, license_number, medical_experience, phone, birth_date, gender, address, created_at)
SELECT
    id,
    '외과',
    'DOC-2024-002',
    12,
    '010-1234-5678',
    '1985-05-10',
    '남성',
    '서울특별시 서초구',
    NOW()
FROM users
WHERE user_id = 'doctor02';

-- 8. 의사 프로필 생성 (doctor03용 - 신경외과)
INSERT IGNORE INTO doctor_profiles (user_id, specialty, license_number, medical_experience, phone, birth_date, gender, address, created_at)
SELECT
    id,
    '신경외과',
    'DOC-2024-003',
    15,
    '010-2345-6789',
    '1982-11-18',
    '남성',
    '서울특별시 송파구',
    NOW()
FROM users
WHERE user_id = 'doctor03';

-- 9. 의사 프로필 생성 (doctor04용 - 피부과)
INSERT IGNORE INTO doctor_profiles (user_id, specialty, license_number, medical_experience, phone, birth_date, gender, address, created_at)
SELECT
    id,
    '피부과',
    'DOC-2024-004',
    8,
    '010-3456-7890',
    '1990-03-25',
    '여성',
    '서울특별시 강동구',
    NOW()
FROM users
WHERE user_id = 'doctor04';

-- ============================================
-- 검증 쿼리
-- ============================================
-- SELECT * FROM users WHERE user_id IN ('user01', 'doctor01');
-- SELECT u.user_id, u.name, u.role, dp.specialty, dp.license_number
-- FROM users u
-- LEFT JOIN doctor_profiles dp ON dp.user_id = u.id
-- WHERE u.user_id IN ('user01', 'doctor01');
