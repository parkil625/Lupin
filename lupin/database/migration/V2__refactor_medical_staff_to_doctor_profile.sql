-- ============================================
-- Migration: MedicalStaff -> DoctorProfile
-- Date: 2025-11-25
-- Description:
--   - MedicalStaff 테이블을 doctor_profiles로 리팩토링
--   - User와 1:1 관계로 변경
--   - 의사도 직원으로 통합 (User.role=DOCTOR)
-- ============================================

-- Step 1: doctor_profiles 테이블 생성
CREATE TABLE IF NOT EXISTS doctor_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    specialty VARCHAR(100) COMMENT '전공 (내과, 외과, 정형외과 등)',
    license_number VARCHAR(50) COMMENT '의사 면허번호',
    medical_experience INT COMMENT '경력 (년)',
    phone VARCHAR(20) COMMENT '연락처',
    birth_date DATE COMMENT '생년월일',
    gender VARCHAR(10) COMMENT '성별',
    address VARCHAR(500) COMMENT '주소',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_doctor_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_doctor_profile_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='의사 프로필 (User와 1:1)';

-- Step 2: 기존 medical_staff 데이터가 있다면 User로 마이그레이션
-- (선택 사항: 기존 데이터가 없다면 생략 가능)
--
-- INSERT INTO users (user_id, email, password, real_name, role, phone, birth_date, gender, current_points, monthly_points, monthly_likes)
-- SELECT
--     user_id,
--     email,
--     password,
--     real_name,
--     'DOCTOR',
--     phone,
--     birth_date,
--     gender,
--     0,
--     0,
--     0
-- FROM medical_staff
-- WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = medical_staff.email);
--
-- INSERT INTO doctor_profiles (user_id, specialty, phone, birth_date, gender, address, created_at, updated_at)
-- SELECT
--     u.id,
--     ms.specialty,
--     ms.phone,
--     ms.birth_date,
--     ms.gender,
--     ms.address,
--     NOW(),
--     NULL
-- FROM medical_staff ms
-- INNER JOIN users u ON u.email = ms.email
-- WHERE NOT EXISTS (SELECT 1 FROM doctor_profiles dp WHERE dp.user_id = u.id);

-- Step 3: medical_staff 테이블 삭제 (데이터 마이그레이션 완료 후)
DROP TABLE IF EXISTS medical_staff;

-- Step 4: 사용하지 않는 prize_claim 테이블 삭제 (PrizeType enum 없어서 사용 불가)
-- DROP TABLE IF EXISTS prize_claim;

-- ============================================
-- 검증 쿼리
-- ============================================
-- SELECT COUNT(*) AS doctor_profile_count FROM doctor_profiles;
-- SELECT u.user_id, u.real_name, u.role, dp.specialty, dp.license_number
-- FROM users u
-- LEFT JOIN doctor_profiles dp ON dp.user_id = u.id
-- WHERE u.role = 'DOCTOR';
