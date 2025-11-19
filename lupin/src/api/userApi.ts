import apiClient from './client';

/**
 * 사용자 관련 API 요청
 */
export const userApi = {
  /**
   * 모든 사용자 조회 (페이징)
   */
  getAllUsers: async (page: number = 0, size: number = 20) => {
    const response = await apiClient.get('/users', {
      params: { page, size },
    });
    return response.data;
  },

  /**
   * 특정 사용자 조회
   */
  getUserById: async (userId: number) => {
    const response = await apiClient.get(`/users/${userId}`);
    return response.data;
  },

  /**
   * 사용자 생성
   */
  createUser: async (userData: any) => {
    const response = await apiClient.post('/users', userData);
    return response.data;
  },

  /**
   * 사용자 정보 수정
   */
  updateUser: async (userId: number, userData: any) => {
    const response = await apiClient.put(`/users/${userId}`, userData);
    return response.data;
  },

  /**
   * 사용자 삭제
   */
  deleteUser: async (userId: number) => {
    const response = await apiClient.delete(`/users/${userId}`);
    return response.data;
  },

  /**
   * 이메일로 사용자 조회
   */
  getUserByEmail: async (email: string) => {
    const response = await apiClient.get('/users/email', {
      params: { email },
    });
    return response.data;
  },

  /**
   * 부서별 사용자 조회
   */
  getUsersByDepartment: async (department: string, page: number = 0, size: number = 20) => {
    const response = await apiClient.get('/users/department', {
      params: { department, page, size },
    });
    return response.data;
  },

  /**
   * 역할별 사용자 조회
   */
  getUsersByRole: async (role: string, page: number = 0, size: number = 20) => {
    const response = await apiClient.get('/users/role', {
      params: { role, page, size },
    });
    return response.data;
  },

  /**
   * 사용자 포인트 조회
   */
  getUserPoints: async (userId: number) => {
    const response = await apiClient.get(`/users/${userId}/points`);
    return response.data;
  },

  /**
   * 상위 포인트 사용자 조회 (랭킹)
   */
  getTopUsersByPoints: async (limit: number = 10) => {
    const response = await apiClient.get('/users/top', {
      params: { limit },
    });
    return response.data;
  },

  /**
   * 특정 사용자 주변 랭킹 조회
   */
  getUserRankingContext: async (userId: number) => {
    const response = await apiClient.get(`/users/${userId}/ranking/context`);
    return response.data;
  },

  /**
   * 전체 통계 조회
   */
  getStatistics: async () => {
    const response = await apiClient.get('/users/statistics');
    return response.data;
  },
};
