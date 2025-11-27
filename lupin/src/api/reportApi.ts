import apiClient from './client';

export const reportApi = {
  createReport: async (data: { targetType: string; targetId: number; reason: string }) => {
    const response = await apiClient.post('/reports', data);
    return response.data;
  },

  getReports: async () => {
    const response = await apiClient.get('/reports');
    return response.data;
  },

  getReportById: async (reportId: number) => {
    const response = await apiClient.get(`/reports/${reportId}`);
    return response.data;
  },
};
