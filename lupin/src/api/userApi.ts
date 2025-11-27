import apiClient from './client';

// Mock 데이터 (Storybook/개발용)
const mockUsers = [
  { id: 1, name: '박선일', realName: '박선일', currentPoints: 450, monthlyLikes: 28, department: '개발팀', activeDays: 15, avgScore: 30, rank: 1, profileImage: 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=100&h=100&fit=crop' },
  { id: 2, name: '김운동', realName: '김운동', currentPoints: 420, monthlyLikes: 25, department: '마케팅팀', activeDays: 14, avgScore: 30, rank: 2, profileImage: 'https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=100&h=100&fit=crop' },
  { id: 3, name: '이헬스', realName: '이헬스', currentPoints: 390, monthlyLikes: 22, department: '디자인팀', activeDays: 13, avgScore: 30, rank: 3, profileImage: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop' },
  { id: 4, name: '박피트', realName: '박피트', currentPoints: 360, monthlyLikes: 20, department: '영업팀', activeDays: 12, avgScore: 30, rank: 4, profileImage: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&h=100&fit=crop' },
  { id: 5, name: '최건강', realName: '최건강', currentPoints: 330, monthlyLikes: 18, department: '인사팀', activeDays: 11, avgScore: 30, rank: 5, profileImage: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop' },
  { id: 6, name: '정활력', realName: '정활력', currentPoints: 300, monthlyLikes: 15, department: '재무팀', activeDays: 10, avgScore: 30, rank: 6, profileImage: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&h=100&fit=crop' },
  { id: 7, name: '한체력', realName: '한체력', currentPoints: 270, monthlyLikes: 12, department: '기획팀', activeDays: 9, avgScore: 30, rank: 7, profileImage: 'https://images.unsplash.com/photo-1552058544-f2b08422138a?w=100&h=100&fit=crop' },
  { id: 8, name: '오근육', realName: '오근육', currentPoints: 240, monthlyLikes: 10, department: '연구팀', activeDays: 8, avgScore: 30, rank: 8, profileImage: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop' },
  { id: 9, name: '강스포츠', realName: '강스포츠', currentPoints: 210, monthlyLikes: 8, department: '품질팀', activeDays: 7, avgScore: 30, rank: 9, profileImage: 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=100&h=100&fit=crop' },
  { id: 10, name: '신헬시', realName: '신헬시', currentPoints: 180, monthlyLikes: 6, department: '총무팀', activeDays: 6, avgScore: 30, rank: 10, profileImage: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=100&h=100&fit=crop' },
  // 11~24위 추가 (Storybook 랭킹 페이지용)
  { id: 11, name: '김달리기', realName: '김달리기', currentPoints: 165, monthlyLikes: 5, department: '영업팀', activeDays: 5, avgScore: 28, rank: 11, profileImage: 'https://images.unsplash.com/photo-1599566150163-29194dcabd36?w=100&h=100&fit=crop' },
  { id: 12, name: '이수영', realName: '이수영', currentPoints: 150, monthlyLikes: 5, department: '마케팅팀', activeDays: 5, avgScore: 27, rank: 12, profileImage: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=100&h=100&fit=crop' },
  { id: 13, name: '박요가', realName: '박요가', currentPoints: 140, monthlyLikes: 4, department: '디자인팀', activeDays: 5, avgScore: 26, rank: 13, profileImage: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=100&h=100&fit=crop' },
  { id: 14, name: '최테니스', realName: '최테니스', currentPoints: 130, monthlyLikes: 4, department: '개발팀', activeDays: 4, avgScore: 25, rank: 14, profileImage: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=100&h=100&fit=crop' },
  { id: 15, name: '정배드', realName: '정배드', currentPoints: 120, monthlyLikes: 4, department: '인사팀', activeDays: 4, avgScore: 24, rank: 15, profileImage: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=100&h=100&fit=crop' },
  { id: 16, name: '한축구', realName: '한축구', currentPoints: 115, monthlyLikes: 3, department: '재무팀', activeDays: 4, avgScore: 23, rank: 16, profileImage: 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=100&h=100&fit=crop' },
  { id: 17, name: '오농구', realName: '오농구', currentPoints: 110, monthlyLikes: 3, department: '기획팀', activeDays: 4, avgScore: 22, rank: 17, profileImage: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=100&h=100&fit=crop' },
  { id: 18, name: '강야구', realName: '강야구', currentPoints: 105, monthlyLikes: 3, department: '연구팀', activeDays: 3, avgScore: 21, rank: 18, profileImage: 'https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?w=100&h=100&fit=crop' },
  { id: 19, name: '신골프', realName: '신골프', currentPoints: 100, monthlyLikes: 3, department: '품질팀', activeDays: 3, avgScore: 20, rank: 19, profileImage: 'https://images.unsplash.com/photo-1463453091185-61582044d556?w=100&h=100&fit=crop' },
  { id: 20, name: '김등산', realName: '김등산', currentPoints: 95, monthlyLikes: 2, department: '총무팀', activeDays: 3, avgScore: 19, rank: 20, profileImage: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=100&h=100&fit=crop' },
  { id: 21, name: '이사이클', realName: '이사이클', currentPoints: 90, monthlyLikes: 2, department: '개발팀', activeDays: 3, avgScore: 18, rank: 21, profileImage: 'https://images.unsplash.com/photo-1557862921-37829c790f19?w=100&h=100&fit=crop' },
  { id: 22, name: '박복싱', realName: '박복싱', currentPoints: 85, monthlyLikes: 2, department: '마케팅팀', activeDays: 3, avgScore: 17, rank: 22, profileImage: 'https://images.unsplash.com/photo-1548142813-c348350df52b?w=100&h=100&fit=crop' },
  { id: 23, name: '나건강', realName: '나건강', currentPoints: 80, monthlyLikes: 2, department: '디자인팀', activeDays: 2, avgScore: 16, rank: 23, profileImage: 'https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=100&h=100&fit=crop' },
  { id: 24, name: '최필라', realName: '최필라', currentPoints: 75, monthlyLikes: 1, department: '영업팀', activeDays: 2, avgScore: 15, rank: 24, profileImage: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=100&h=100&fit=crop' },
];

export const userApi = {
  getCurrentUser: async () => {
    try {
      const response = await apiClient.get('/users/me');
      return response.data;
    } catch {
      return mockUsers[0];
    }
  },

  getUserById: async (userId: number) => {
    try {
      const response = await apiClient.get(`/users/${userId}`);
      return response.data;
    } catch {
      return mockUsers.find(u => u.id === userId) || mockUsers[0];
    }
  },

  updateUser: async (userId: number, data: any) => {
    try {
      const response = await apiClient.put(`/users/${userId}`, data);
      return response.data;
    } catch {
      return { ...mockUsers[0], ...data };
    }
  },

  getTopUsersByPoints: async (limit = 10) => {
    try {
      const response = await apiClient.get(`/users/ranking?limit=${limit}`);
      return response.data;
    } catch {
      return mockUsers.slice(0, limit).map((u, i) => ({ ...u, points: u.currentPoints, rank: i + 1 }));
    }
  },

  getUserRankingContext: async (userId: number) => {
    try {
      const response = await apiClient.get(`/users/${userId}/ranking-context`);
      return response.data;
    } catch {
      const userIndex = mockUsers.findIndex(u => u.id === userId);
      if (userIndex === -1) return [{ ...mockUsers[0], id: userId, rank: 11 }];
      const start = Math.max(0, userIndex - 1);
      const end = Math.min(mockUsers.length, userIndex + 2);
      return mockUsers.slice(start, end).map((u, i) => ({ ...u, rank: start + i + 1, points: u.currentPoints }));
    }
  },

  getUserStats: async (userId: number) => {
    try {
      const response = await apiClient.get(`/users/${userId}/stats`);
      return response.data;
    } catch {
      const user = mockUsers.find(u => u.id === userId) || mockUsers[0];
      return { activeDays: user.activeDays, avgScore: user.avgScore, streak: 5 };
    }
  },

  getStatistics: async () => {
    try {
      const response = await apiClient.get('/users/statistics');
      return response.data;
    } catch {
      return { totalUsers: 150, activeUsersThisMonth: 85, averagePoints: 280 };
    }
  },

  updateAvatar: async (userId: number, avatarUrl: string) => {
    try {
      const response = await apiClient.put(`/users/${userId}/avatar`, { avatar: avatarUrl });
      return response.data;
    } catch {
      return { ...mockUsers[0], avatar: avatarUrl };
    }
  },
};
