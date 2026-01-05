import apiClient from './client';

/**
 * 채팅 메시지 응답 DTO
 * 백엔드 ChatMessageResponse.java와 일치하도록 수정
 */
export interface ChatMessageResponse {
  id: number;
  roomId: string;      // 🔧 수정: String으로 변경 ("appointment_123" 형식)
  senderId: number;
  senderName: string;
  content: string;
  sentAt: string;      // 🔧 수정: createdAt 제거 (백엔드에 없음)
  isRead: boolean;
}

/**
 * 채팅방 응답 DTO
 * [N+1 문제 해결] 백엔드에서 환자 프로필 정보 포함하여 전달
 */
export interface ChatRoomResponse {
  roomId: string;           // "appointment_123"
  patientId: number;
  patientName: string;
  patientAvatar?: string;   // [N+1 해결] 백엔드에서 함께 전달
  patientDepartment?: string; // [N+1 해결] 백엔드에서 함께 전달
  doctorId: number;
  lastMessage?: string;
  unreadCount: number;
  lastMessageTime?: string;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'UNKNOWN';
  appointmentTime?: string;
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

  /**
   * 채팅 기록 조회
   * 🔧 수정: 백엔드 엔드포인트에 맞춤 (/api/chat/history/{roomId})
   */
  getAllMessagesByRoomId: async (roomId: string): Promise<ChatMessageResponse[]> => {
    try {
      const response = await apiClient.get(`/chat/history/${roomId}`);
      return response.data;
    } catch (error) {
      console.error('채팅 기록 조회 실패:', error);
      throw error;  // 🔧 수정: 에러를 상위로 전파 (Silent Failure 제거)
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

  /**
   * 읽음 처리
   * 🔧 수정: roomId 타입 변경 (number → string)
   */
  markAsRead: async (roomId: string, userId: number): Promise<void> => {
    try {
      await apiClient.put(`/chat/rooms/${roomId}/read?userId=${userId}`);
    } catch (error) {
      console.error('읽음 처리 실패:', error);
      throw error;  // 🔧 수정: Silent Failure 제거
    }
  },
};
