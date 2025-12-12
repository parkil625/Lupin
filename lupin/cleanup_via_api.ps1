# API를 통한 테스트 데이터 정리
Write-Host "테스트 데이터 정리 시작..." -ForegroundColor Green

$baseUrl = "http://localhost:8080/api/test"

Write-Host "`n1. 모든 채팅 메시지 삭제..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/cleanup/messages" -Method Delete
    Write-Host "삭제: $($response.deleted)개, 남은 메시지: $($response.remaining)개" -ForegroundColor Cyan
} catch {
    Write-Host "에러: $_" -ForegroundColor Red
}

Write-Host "`n2. 테스트 예약 데이터 삭제..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/cleanup/appointments" -Method Delete
    Write-Host "삭제: $($response.deleted)개, 남은 예약: $($response.remaining)개" -ForegroundColor Cyan
} catch {
    Write-Host "에러: $_" -ForegroundColor Red
}

Write-Host "`n✅ 데이터 정리 완료!" -ForegroundColor Green
