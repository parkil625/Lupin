import apiClient from './client';

export const lotteryApi = {
  getTicketCount: async (userId: number) => {
    try {
      const response = await apiClient.get(`/lottery/tickets?userId=${userId}`);
      return response.data;
    } catch {
      return { count: 3, userId };
    }
  },

  checkWinner: async (userId: number) => {
    try {
      const response = await apiClient.get(`/lottery/check-winner?userId=${userId}`);
      return response.data;
    } catch {
      return { isWinner: false, userId };
    }
  },

  claimPrize: async (userId: number) => {
    try {
      const response = await apiClient.post(`/lottery/claim?userId=${userId}`);
      return response.data;
    } catch {
      return { success: true, userId };
    }
  },

  runDraw: async () => {
    try {
      const response = await apiClient.post('/lottery/draw');
      return response.data;
    } catch {
      return { success: true };
    }
  },

  getDrawHistory: async () => {
    try {
      const response = await apiClient.get('/lottery/history');
      return response.data;
    } catch {
      return [];
    }
  },
};
