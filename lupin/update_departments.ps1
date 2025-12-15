# 의사 진료과 업데이트 스크립트
Write-Host "의사 진료과 업데이트 시작..." -ForegroundColor Green

$mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$dbHost = "localhost"
$dbPort = "3306"
$dbName = "lupin_db"
$dbUser = "root"
$dbPassword = "lupin1234"

$sqlFile = "c:\Lupin\lupin\update_doctor_departments.sql"

Write-Host "SQL 실행 중..." -ForegroundColor Yellow
& $mysqlPath -h $dbHost -P $dbPort -u $dbUser -p$dbPassword $dbName -e "source $sqlFile"

Write-Host "`n✅ 업데이트 완료!" -ForegroundColor Green
