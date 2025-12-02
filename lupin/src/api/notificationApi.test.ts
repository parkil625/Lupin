import { describe, it, expect, vi, beforeEach } from "vitest";
import { notificationApi } from "./notificationApi";
import apiClient from "./client";

vi.mock("./client", () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("notificationApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("getAllNotifications", () => {
    it("should call GET /notifications", async () => {
      const mockResponse = {
        data: [
          { id: 1, message: "알림1", isRead: false },
          { id: 2, message: "알림2", isRead: true },
        ],
      };
      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await notificationApi.getAllNotifications();

      expect(apiClient.get).toHaveBeenCalledWith("/notifications");
      expect(result).toHaveLength(2);
      expect(result[0].message).toBe("알림1");
    });

    it("should return empty array when data is null", async () => {
      const mockResponse = { data: null };
      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await notificationApi.getAllNotifications();

      expect(result).toEqual([]);
    });

    it("should return empty array on error", async () => {
      vi.mocked(apiClient.get).mockRejectedValue(new Error("Network error"));

      const result = await notificationApi.getAllNotifications();

      expect(result).toEqual([]);
    });
  });

  describe("markAsRead", () => {
    it("should call PATCH /notifications/{id}/read", async () => {
      const mockResponse = { data: { id: 1, isRead: true } };
      vi.mocked(apiClient.patch).mockResolvedValue(mockResponse);

      const result = await notificationApi.markAsRead(1);

      expect(apiClient.patch).toHaveBeenCalledWith("/notifications/1/read");
      expect(result.isRead).toBe(true);
    });

    it("should return success on error", async () => {
      vi.mocked(apiClient.patch).mockRejectedValue(new Error("Error"));

      const result = await notificationApi.markAsRead(1);

      expect(result).toEqual({ success: true });
    });
  });

  describe("markAllAsRead", () => {
    it("should call PATCH /notifications/read-all", async () => {
      const mockResponse = { data: { updated: 5 } };
      vi.mocked(apiClient.patch).mockResolvedValue(mockResponse);

      const result = await notificationApi.markAllAsRead();

      expect(apiClient.patch).toHaveBeenCalledWith("/notifications/read-all");
      expect(result.updated).toBe(5);
    });

    it("should return success on error", async () => {
      vi.mocked(apiClient.patch).mockRejectedValue(new Error("Error"));

      const result = await notificationApi.markAllAsRead();

      expect(result).toEqual({ success: true });
    });
  });

  describe("deleteNotification", () => {
    it("should call DELETE /notifications/{id}", async () => {
      const mockResponse = { data: { deleted: true } };
      vi.mocked(apiClient.delete).mockResolvedValue(mockResponse);

      const result = await notificationApi.deleteNotification(1);

      expect(apiClient.delete).toHaveBeenCalledWith("/notifications/1");
      expect(result.deleted).toBe(true);
    });

    it("should return success on error", async () => {
      vi.mocked(apiClient.delete).mockRejectedValue(new Error("Error"));

      const result = await notificationApi.deleteNotification(1);

      expect(result).toEqual({ success: true });
    });
  });
});
