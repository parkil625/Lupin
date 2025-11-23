import apiClient from './client';

export interface LoginRequest {
  email: string;  // Backend still uses 'email' field name
  password: string;
}

export interface LoginResponse {
    accessToken: string;
    refreshToken?: string; // 백엔드에 있어서 추가함 (선택적)
    tokenType?: string;    // 백엔드에 없으면 물음표(?) 붙여야 안전함
    id: number;            // [핵심 수정] userId -> id 로 변경
    userId: string;
    email: string;
    name: string;
    role: string;
}

/**
 * 인증 API
 */
export const authApi = {
  /**
   * 로그인
   */
  login: async (username: string, password: string): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/auth/login', {
      email: username,  // Send as 'email' field but value is username (user01, user02, etc.)
      password,
    });
    return response.data;
  },

  /**
   * 구글 로그인
   */
  googleLogin: async (googleToken: string): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/auth/google', {
      token: googleToken,
    });
    return response.data;
  },
};
