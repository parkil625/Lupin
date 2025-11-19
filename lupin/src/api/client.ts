import axios from 'axios';

/**
 * Axios 인스턴스 생성
 * 백엔드 API와 통신하기 위한 기본 설정
 */
const apiClient = axios.create({
  baseURL: 'http://localhost:8081/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

/**
 * 요청 인터셉터
 * 모든 요청에 대해 로깅 및 인증 토큰 추가 가능
 */
apiClient.interceptors.request.use(
  (config) => {
    // JWT 토큰 추가
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * 응답 인터셉터
 * 에러 처리 및 응답 데이터 가공
 */
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // 에러 처리
    if (error.response) {
      // 서버가 2xx 외의 상태 코드로 응답
      console.error('API Error:', error.response.status, error.response.data);
    } else if (error.request) {
      // 요청은 보냈지만 응답을 받지 못함
      console.error('No response received:', error.request);
    } else {
      // 요청 설정 중 에러 발생
      console.error('Error setting up request:', error.message);
    }
    return Promise.reject(error);
  }
);

export default apiClient;
