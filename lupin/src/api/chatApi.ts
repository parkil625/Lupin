import apiClient from './client';

export interface ChatMessageResponse {
  id: number;
  roomId: number;
  senderId: number;
  senderName: string;
  content: string;
  createdAt: string;
  sentAt: string;
  isRead: boolean;
}

export const chatApi = {
  getChatRooms: async (userId: number) => {
    try {
      const response = await apiClient.get(`/chat/rooms?userId=${userId}`);
      return response.data;
    } catch {
      return [];
    }
  },

  getChatMessages: async (roomId: number) => {
    try {
      const response = await apiClient.get(`/chat/rooms/${roomId}/messages`);
      return response.data;
    } catch {
      return [];
    }
  },

  getAllMessagesByRoomId: async (roomId: string) => {
    try {
      const response = await apiClient.get(`/chat/rooms/${roomId}/messages`);
      return response.data;
    } catch {
      return [
        { id: 1, roomId, senderId: 21, senderName: '김의사', content: '안녕하세요, 오늘 상담 도와드리겠습니다.', sentAt: new Date(Date.now() - 60000).toISOString(), isRead: true },
        { id: 2, roomId, senderId: 1, senderName: '박선일', content: '네, 안녕하세요. 최근 두통이 있어서요.', sentAt: new Date(Date.now() - 30000).toISOString(), isRead: true },
      ];
    }
  },

  sendMessage: async (roomId: number, content: string) => {
    try {
      const response = await apiClient.post(`/chat/rooms/${roomId}/messages`, { content });
      return response.data;
    } catch {
      return { id: Date.now(), roomId, content, sentAt: new Date().toISOString() };
    }
  },

  createChatRoom: async (doctorId: number, patientId: number) => {
    try {
      const response = await apiClient.post('/chat/rooms', { doctorId, patientId });
      return response.data;
    } catch {
      return { id: `${patientId}:${doctorId}`, doctorId, patientId };
    }
  },

  markAsRead: async (roomId: number, userId: number) => {
    try {
      const response = await apiClient.put(`/chat/rooms/${roomId}/read?userId=${userId}`);
      return response.data;
    } catch {
      return { success: true };
    }
  },
};
