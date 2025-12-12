# MySQL 데이터 정리 스크립트
# 실행 방법: powershell -ExecutionPolicy Bypass -File cleanup_data.ps1

Write-Host "데이터 정리 스크립트 시작..." -ForegroundColor Green

# MySQL 경로 설정 (일반적인 설치 경로)
$mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

# MySQL 연결 정보 (application.yml 기준)
$dbHost = "localhost"
$dbPort = "3306"
$dbName = "lupin_db"
$dbUser = "root"
$dbPassword = "lupin1234"

# 정리할 SQL 명령어
$sqlCommands = @"
-- 1. 채팅 메시지 전부 삭제
DELETE FROM chat_messages;

-- 2. 예약 데이터 삭제
DELETE FROM appointments WHERE doctor_id IN (22, 23, 24, 25);

-- 3. 결과 확인
SELECT COUNT(*) AS remaining_messages FROM chat_messages;
SELECT COUNT(*) AS remaining_appointments FROM appointments;
"@

Write-Host "MySQL에 연결 중..." -ForegroundColor Yellow
Write-Host "실행할 SQL:" -ForegroundColor Cyan
Write-Host $sqlCommands

# SQL 파일 생성
$sqlFile = "c:\Lupin\lupin\temp_cleanup.sql"
$sqlCommands | Out-File -FilePath $sqlFile -Encoding UTF8

# MySQL 명령 실행
& $mysqlPath -h $dbHost -P $dbPort -u $dbUser -p$dbPassword $dbName -e "source $sqlFile"

# 임시 파일 삭제
Remove-Item $sqlFile

Write-Host "`n데이터 정리 완료!" -ForegroundColor Green
