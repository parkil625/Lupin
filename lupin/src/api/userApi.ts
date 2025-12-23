import apiClient from "./client";
import { Member } from "@/types/dashboard.types";

export const userApi = {
  getCurrentUser: async () => {
    const response = await apiClient.get("/users/me");
    return response.data;
  },

  getUserById: async (userId: number) => {
    const response = await apiClient.get(`/users/${userId}`);
    return response.data;
  },

  updateUser: async (userId: number, data: Record<string, unknown>) => {
    const response = await apiClient.put(`/users/${userId}`, data);
    return response.data;
  },

  getTopUsersByPoints: async (limit = 10) => {
    const response = await apiClient.get(`/users/ranking?limit=${limit}`);
    return response.data;
  },

  getUserRankingContext: async (userId: number) => {
    const response = await apiClient.get(`/users/${userId}/ranking-context`);
    return response.data;
  },

  getUserStats: async (userId: number) => {
    const response = await apiClient.get(`/users/${userId}/stats`);
    return response.data;
  },

  getStatistics: async () => {
    const response = await apiClient.get("/users/statistics");
    return response.data;
  },

  updateAvatar: async (userId: number, avatarUrl: string) => {
    const response = await apiClient.put(`/users/${userId}/avatar`, {
      avatar: avatarUrl,
    });
    return response.data;
  },

  // 진료과별 의사 조회
  getDoctorsByDepartment: async (
    department: string
  ): Promise<Array<{ id: number; name: string; department: string }>> => {
    const response = await apiClient.get(
      `/users/doctors?department=${department}`
    );
    return response.data;
  },

  getDoctorPatients: async (doctorId: number): Promise<Member[]> => {
    // 실제 엔드포인트에 맞게 수정이 필요할 수 있습니다.
    const response = await apiClient.get(`/users/doctor/${doctorId}/patients`);
    return response.data;
  },
};
