# 배포 서버 테스트 스크립트
$BASE_URL = "https://www.lupin-care.com"

Write-Host "=== 1단계: 로그인 ===" -ForegroundColor Yellow
$loginBody = @{
    email = "user01"
    password = "1"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/api/auth/login" `
        -Method Post `
        -ContentType "application/json" `
        -Body $loginBody

    $accessToken = $loginResponse.accessToken
    Write-Host "✅ 로그인 성공! 토큰: $($accessToken.Substring(0,20))..." -ForegroundColor Green
    Write-Host "   사용자: $($loginResponse.name) (ID: $($loginResponse.userId), Role: $($loginResponse.role))" -ForegroundColor Cyan
} catch {
    Write-Host "❌ 로그인 실패: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== 2단계: 예약 생성 ===" -ForegroundColor Yellow
$appointmentBody = @{
    patientId = 13
    doctorId = 14
    date = "2025-12-11T10:00:00"
} | ConvertTo-Json

try {
    $headers = @{
        "Authorization" = "Bearer $accessToken"
        "Content-Type" = "application/json"
    }

    $appointmentResponse = Invoke-RestMethod -Uri "$BASE_URL/api/appointment" `
        -Method Post `
        -Headers $headers `
        -Body $appointmentBody

    Write-Host "✅ 예약 성공! 예약 ID: $appointmentResponse" -ForegroundColor Green
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorBody = $_.ErrorDetails.Message

    Write-Host "❌ 예약 실패 (HTTP $statusCode)" -ForegroundColor Red
    Write-Host "에러 상세: $errorBody" -ForegroundColor Red

    if ($statusCode -eq 401) {
        Write-Host "`n[원인 분석]" -ForegroundColor Cyan
        Write-Host "1. 토큰이 만료되었거나" -ForegroundColor Yellow
        Write-Host "2. 토큰 형식이 잘못되었거나" -ForegroundColor Yellow
        Write-Host "3. SecurityConfig에서 /api/appointment를 인증 필수로 설정했습니다." -ForegroundColor Yellow
        Write-Host "`n[확인사항]" -ForegroundColor Cyan
        Write-Host "- JwtAuthenticationFilter가 토큰을 제대로 파싱하는지" -ForegroundColor White
        Write-Host "- Authorization 헤더가 'Bearer [token]' 형식인지" -ForegroundColor White
    }
}

Write-Host "`n=== 테스트 완료 ===" -ForegroundColor Yellow
