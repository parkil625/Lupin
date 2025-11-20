import apiClient from './client';

/**
 * 추첨권 관련 API 요청
 */
export const lotteryApi = {
  /**
   * 사용자의 미사용 추첨권 개수 조회
   */
  getUnusedTicketCount: async (userId: number) => {
    const response = await apiClient.get(`/lottery/users/${userId}/unused/count`);
    return response.data;
  },

  /**
   * 사용자의 미사용 추첨권 목록 조회
   */
  getUnusedTickets: async (userId: number) => {
    const response = await apiClient.get(`/lottery/users/${userId}/unused`);
    return response.data;
  },

  /**
   * 사용자의 모든 추첨권 조회
   */
  getAllTickets: async (userId: number) => {
    const response = await apiClient.get(`/lottery/users/${userId}/all`);
    return response.data;
  },

  /**
   * 추첨권 사용 (추첨 진행)
   */
  useTicket: async (ticketId: number) => {
    const response = await apiClient.post(`/lottery/tickets/${ticketId}/use`);
    return response.data;
  },

  /**
   * 사용자의 당첨 내역 조회
   */
  getWinningTickets: async (userId: number) => {
    const response = await apiClient.get(`/lottery/users/${userId}/wins`);
    return response.data;
  },

  /**
   * 사용자의 상금 수령 신청 내역 조회
   */
  getPrizeClaims: async (userId: number) => {
    const response = await apiClient.get(`/lottery/users/${userId}/claims`);
    return response.data;
  },

  /**
   * 상금 수령 신청
   */
  claimPrize: async (ticketId: number, bankName: string, accountNumber: string, accountHolder: string) => {
    const response = await apiClient.post(`/lottery/tickets/${ticketId}/claim`, null, {
      params: { bankName, accountNumber, accountHolder }
    });
    return response.data;
  },

  /**
   * 수동 추첨 실행 (테스트용)
   */
  runManualDraw: async () => {
    const response = await apiClient.post('/lottery/draw');
    return response.data;
  },
};
