import apiClient from './client';

/**
 * 추첨권 관련 API 요청
 */
export const lotteryApi = {
  /**
   * 추첨권 발급
   */
  issueTicket: async (userId: number) => {
    const response = await apiClient.post('/lottery/issue', null, {
      params: { userId }
    });
    return response.data;
  },

  /**
   * 사용자의 추첨권 목록 조회
   */
  getTickets: async (userId: number) => {
    const response = await apiClient.get(`/lottery/users/${userId}`);
    return response.data;
  },

  /**
   * 사용자의 추첨권 개수 조회
   */
  getTicketCount: async (userId: number) => {
    const response = await apiClient.get(`/lottery/users/${userId}/count`);
    return response.data;
  },
};
