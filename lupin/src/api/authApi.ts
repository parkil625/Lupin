import apiClient from './client';

export interface LoginRequest {
  email: string;  // Backend still uses 'email' field name
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  userId: number;
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
};
