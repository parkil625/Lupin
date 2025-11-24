# k6 부하 테스트

## 설치

### Windows
```bash
choco install k6
```

### macOS
```bash
brew install k6
```

### Docker
```bash
docker pull grafana/k6
```

## 실행 방법

### 기본 부하 테스트
```bash
k6 run k6/load-test.js
```

### 스트레스 테스트 (한계점 찾기)
```bash
k6 run k6/stress-test.js
```

### 환경 변수로 URL 지정
```bash
k6 run -e BASE_URL=http://api.lupin-care.com k6/load-test.js
```

### 가상 사용자 수 조정
```bash
k6 run --vus 50 --duration 30s k6/load-test.js
```

## CI/CD 통합 (GitHub Actions)

```yaml
- name: Run k6 Load Test
  uses: grafana/k6-action@v0.3.1
  with:
    filename: k6/load-test.js
    flags: --out json=k6-results.json
```

## 테스트 시나리오

### load-test.js
- 일반적인 사용자 시나리오
- 헬스 체크 → 로그인 → 피드 조회 → 검색
- 임계값: 95%ile < 500ms, 실패율 < 1%

### stress-test.js
- 서버 한계점 탐색
- 100명 → 200명 → 300명 점진적 증가
- 임계값: 99%ile < 1500ms, 실패율 < 10%

## 결과 해석

```
응답 시간:
  - 평균: 50ms (좋음: < 100ms)
  - p(95): 150ms (좋음: < 500ms)
  - p(99): 300ms (좋음: < 1000ms)

초당 요청: 500/s (서버 처리량)
실패율: 0.5% (좋음: < 1%)
```

## 성능 개선 포인트

1. **응답 시간 느림**: DB 쿼리 최적화, 인덱스 추가
2. **실패율 높음**: 커넥션 풀 증가, 타임아웃 조정
3. **처리량 낮음**: 서버 스케일링, 캐시 적용
