import apiClient from './client';
import { Notification } from '../types/dashboard.types';

/**
 * 알림 관련 API 요청
 */
export const notificationApi = {
  /**
   * 모든 알림 조회
   */
  getAllNotifications: async (userId: number) => {
    const response = await apiClient.get(`/notifications/users/${userId}/all`);
    // Backend 응답을 프론트엔드 타입으로 매핑
    const notifications = response.data.map((notif: any) => ({
      ...notif,
      read: notif.isRead,
      time: new Date(notif.createdAt).toLocaleString('ko-KR'),
      feedId: notif.feedId || undefined,
      commentId: notif.commentId || undefined,
      chatRoomId: notif.refType === 'CHAT' && notif.refId ? parseInt(notif.refId) : undefined,
    }));
    return notifications;
  },

  /**
   * 읽지 않은 알림 조회
   */
  getUnreadNotifications: async (userId: number) => {
    const response = await apiClient.get(`/notifications/users/${userId}/unread`);
    return response.data;
  },

  /**
   * 읽지 않은 알림 수 조회
   */
  getUnreadCount: async (userId: number) => {
    const response = await apiClient.get(`/notifications/users/${userId}/unread/count`);
    return response.data;
  },

  /**
   * 알림 읽음 처리
   */
  markAsRead: async (notificationId: number, userId: number) => {
    const response = await apiClient.patch(`/notifications/${notificationId}/read`, null, {
      params: { userId }
    });
    return response.data;
  },

  /**
   * 모든 알림 읽음 처리
   */
  markAllAsRead: async (userId: number) => {
    const response = await apiClient.patch(`/notifications/users/${userId}/read-all`);
    return response.data;
  },

  /**
   * 알림 삭제
   */
  deleteNotification: async (notificationId: number, userId: number) => {
    const response = await apiClient.delete(`/notifications/${notificationId}`, {
      params: { userId }
    });
    return response.data;
  },

  /**
   * 오래된 읽은 알림 삭제 (30일 이상)
   */
  deleteOldReadNotifications: async (userId: number) => {
    const response = await apiClient.delete(`/notifications/users/${userId}/cleanup`);
    return response.data;
  },
};
