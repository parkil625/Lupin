-- ============================================
-- Run All Migrations
-- ============================================

-- Migration V2: MedicalStaff -> DoctorProfile
source database/migration/V2__refactor_medical_staff_to_doctor_profile.sql;

-- Migration V3: Insert Test Users
source database/migration/V3__insert_test_users.sql;

-- ============================================
-- Verification
-- ============================================
SELECT '=== Users ===' AS '';
SELECT user_id, email, real_name, role, department FROM users;

SELECT '=== Doctor Profiles ===' AS '';
SELECT u.user_id, u.real_name, dp.specialty, dp.license_number, dp.medical_experience
FROM users u
INNER JOIN doctor_profiles dp ON dp.user_id = u.id
WHERE u.role = 'DOCTOR';
