# MySQL 데이터베이스 확인 스크립트
$query = @"
USE lupin;

-- 1. prescriptions 테이블 데이터 확인
SELECT COUNT(*) as prescription_count FROM prescriptions;

-- 2. patient_id = 201인 처방전 확인
SELECT * FROM prescriptions WHERE patient_id = 201;

-- 3. users 테이블에서 ID 201 확인
SELECT id, name, role FROM users WHERE id = 201;

-- 4. prescriptions 테이블 구조 확인
DESCRIBE prescriptions;
"@

# MySQL 실행
mysql -u root -p1234 -e $query
