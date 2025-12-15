# API를 통한 의사 진료과 설정
Write-Host "의사 진료과 설정 시작..." -ForegroundColor Green

$baseUrl = "http://localhost:8080/api/setup"

Write-Host "`n1. 현재 의사 목록 조회..." -ForegroundColor Yellow
try {
    $doctors = Invoke-RestMethod -Uri "$baseUrl/doctors" -Method Get
    Write-Host "현재 의사:" -ForegroundColor Cyan
    $doctors | ForEach-Object {
        Write-Host "  ID: $($_.id), 이름: $($_.name), 진료과: $($_.department)" -ForegroundColor White
    }
} catch {
    Write-Host "에러: $_" -ForegroundColor Red
}

Write-Host "`n2. 진료과 설정 실행..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/doctor-departments" -Method Post
    Write-Host "업데이트 완료: $($response.updatedCount)명" -ForegroundColor Cyan
    Write-Host "메시지: $($response.message)" -ForegroundColor Cyan
} catch {
    Write-Host "에러: $_" -ForegroundColor Red
}

Write-Host "`n3. 업데이트 후 확인..." -ForegroundColor Yellow
try {
    $doctors = Invoke-RestMethod -Uri "$baseUrl/doctors" -Method Get
    Write-Host "업데이트 후 의사 목록:" -ForegroundColor Cyan
    $doctors | ForEach-Object {
        Write-Host "  ID: $($_.id), 이름: $($_.name), 진료과: $($_.department)" -ForegroundColor White
    }
} catch {
    Write-Host "에러: $_" -ForegroundColor Red
}

Write-Host "`n✅ 설정 완료!" -ForegroundColor Green
