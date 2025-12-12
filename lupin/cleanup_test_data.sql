-- 테스트 데이터 정리 스크립트

-- 1. 채팅 메시지 전부 삭제
DELETE FROM chat_messages;

-- 2. 예약 데이터 삭제 (의사 ID 22-25의 예약만 삭제)
DELETE FROM appointments WHERE doctor_id IN (22, 23, 24, 25);

-- 3. 확인
SELECT COUNT(*) AS remaining_messages FROM chat_messages;
SELECT COUNT(*) AS remaining_appointments FROM appointments;

-- 4. 현재 예약 상태 확인
SELECT
    a.id,
    a.date,
    a.status,
    p.name AS patient_name,
    d.name AS doctor_name
FROM appointments a
JOIN users p ON a.patient_id = p.id
JOIN users d ON a.doctor_id = d.id
ORDER BY a.date DESC
LIMIT 10;
