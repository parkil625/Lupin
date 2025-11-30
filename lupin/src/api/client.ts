import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

/**
 * Axios 인스턴스 생성
 * 백엔드 API와 통신하기 위한 기본 설정
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8081/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

// 토큰 재발급 중복 요청 방지
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: AxiosError) => void;
}> = [];

const processQueue = (error: AxiosError | null, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token!);
    }
  });
  failedQueue = [];
};

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
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // 401 에러이고 재시도하지 않은 요청인 경우
    if (error.response?.status === 401 && !originalRequest._retry) {
      // 로그인/재발급 요청 자체가 실패한 경우는 재시도하지 않음
      if (originalRequest.url?.includes('/auth/login') ||
          originalRequest.url?.includes('/auth/reissue')) {
        return Promise.reject(error);
      }

      if (isRefreshing) {
        // 이미 재발급 중이면 대기열에 추가
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return apiClient(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Refresh Token으로 재발급 요청 (쿠키로 전송됨)
        const response = await apiClient.post('/auth/reissue');
        const newAccessToken = response.data.accessToken;

        localStorage.setItem('accessToken', newAccessToken);
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

        processQueue(null, newAccessToken);

        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError as AxiosError, null);

        // 재발급 실패 시 로그아웃 처리
        localStorage.removeItem('accessToken');
        localStorage.removeItem('auth-storage');
        window.location.href = '/';

        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // 기타 에러 처리
    if (error.response) {
      console.error('API Error:', error.response.status, error.response.data);
    } else if (error.request) {
      console.error('No response received:', error.request);
    } else {
      console.error('Error setting up request:', error.message);
    }
    return Promise.reject(error);
  }
);

export default apiClient;
