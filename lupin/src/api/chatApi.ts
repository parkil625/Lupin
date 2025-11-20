import apiClient from './client';

/**
 * 채팅 메시지 생성 요청
 */
export interface ChatMessageCreateRequest {
  senderId: number;
  patientId: number;
  doctorId: number;
  content: string;
}

/**
 * 채팅 메시지 응답
 */
export interface ChatMessageResponse {
  id: number;
  roomId: string;
  content: string;
  senderId: number;
  senderName: string;
  senderProfileImage?: string;
  sentAt: string;
  isRead: boolean;
}

/**
 * 채팅방 응답
 */
export interface ChatRoomResponse {
  roomId: string;
  patientId: number;
  patientName: string;
  patientProfileImage?: string;
  doctorId: number;
  doctorName: string;
  doctorProfileImage?: string;
  lastMessage?: string;
  lastMessageTime?: string;
  unreadCount: number;
}

/**
 * 채팅 API
 */
export const chatApi = {
  /**
   * 메시지 전송
   */
  sendMessage: async (request: ChatMessageCreateRequest): Promise<ChatMessageResponse> => {
    const response = await apiClient.post<ChatMessageResponse>('/chat/messages', request);
    return response.data;
  },

  /**
   * 특정 채팅방의 메시지 목록 조회 (전체)
   */
  getAllMessagesByRoomId: async (roomId: string): Promise<ChatMessageResponse[]> => {
    const response = await apiClient.get<ChatMessageResponse[]>(`/chat/rooms/${roomId}/messages/all`);
    return response.data;
  },

  /**
   * 환자와 의사 간의 메시지 조회
   */
  getMessagesBetweenUsers: async (patientId: number, doctorId: number): Promise<ChatMessageResponse[]> => {
    const response = await apiClient.get<ChatMessageResponse[]>('/chat/messages/between', {
      params: { patientId, doctorId }
    });
    return response.data;
  },

  /**
   * 특정 채팅방의 읽지 않은 메시지 수 조회
   */
  getUnreadMessageCount: async (roomId: string, userId: number): Promise<number> => {
    const response = await apiClient.get<number>(`/chat/rooms/${roomId}/unread-count`, {
      params: { userId }
    });
    return response.data;
  },

  /**
   * 특정 채팅방의 메시지 전체 읽음 처리
   */
  markAllAsRead: async (roomId: string, userId: number): Promise<void> => {
    await apiClient.patch(`/chat/rooms/${roomId}/read-all`, null, {
      params: { userId }
    });
  },

  /**
   * 특정 사용자가 참여한 채팅방 목록 조회
   */
  getChatRoomsByUserId: async (userId: number): Promise<ChatRoomResponse[]> => {
    const response = await apiClient.get<ChatRoomResponse[]>(`/chat/rooms/users/${userId}`);
    return response.data;
  },

  /**
   * 메시지 상세 조회
   */
  getMessageDetail: async (messageId: number): Promise<ChatMessageResponse> => {
    const response = await apiClient.get<ChatMessageResponse>(`/chat/messages/${messageId}`);
    return response.data;
  },

  /**
   * 메시지 삭제
   */
  deleteMessage: async (messageId: number, userId: number): Promise<void> => {
    await apiClient.delete(`/chat/messages/${messageId}`, {
      params: { userId }
    });
  },
};
