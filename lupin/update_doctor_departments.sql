-- 의사 진료과 설정
-- ID 22-25는 각각 내과, 외과, 신경정신과, 피부과로 설정

UPDATE users SET department = 'internal' WHERE id = 22 AND role = 'DOCTOR';
UPDATE users SET department = 'surgery' WHERE id = 23 AND role = 'DOCTOR';
UPDATE users SET department = 'psychiatry' WHERE id = 24 AND role = 'DOCTOR';
UPDATE users SET department = 'dermatology' WHERE id = 25 AND role = 'DOCTOR';

-- 확인
SELECT id, name, role, department FROM users WHERE role = 'DOCTOR' AND id BETWEEN 22 AND 25;
