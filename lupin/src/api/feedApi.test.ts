import { describe, it, expect, vi, beforeEach } from "vitest";
import { feedApi } from "./feedApi";
import apiClient from "./client";

vi.mock("./client", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

vi.mock("@/lib/utils", () => ({
  getS3Url: vi.fn((url: string) => `https://s3.example.com/${url}`),
}));

describe("feedApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("getAllFeeds", () => {
    it("should call GET /feeds with pagination params", async () => {
      const mockResponse = {
        data: {
          content: [{ id: 1, images: ["img1.jpg"], writerAvatar: "avatar.jpg" }],
          totalPages: 1,
          totalElements: 1,
        },
      };
      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await feedApi.getAllFeeds(0, 10);

      expect(apiClient.get).toHaveBeenCalledWith("/feeds?page=0&size=10");
      expect(result?.content?.[0]?.images?.[0]).toBe("https://s3.example.com/img1.jpg");
    });

    it("should return empty content on error", async () => {
      vi.mocked(apiClient.get).mockRejectedValue(new Error("Network error"));

      const result = await feedApi.getAllFeeds();

      expect(result).toEqual({ content: [], totalPages: 0, totalElements: 0 });
    });
  });

  describe("getFeedsByUserId", () => {
    it("should call GET /feeds/my", async () => {
      const mockResponse = {
        data: {
          content: [{ id: 1, images: [] }],
          totalPages: 1,
          totalElements: 1,
        },
      };
      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await feedApi.getFeedsByUserId(1, 0, 10);

      expect(apiClient.get).toHaveBeenCalledWith("/feeds/my?page=0&size=10");
      expect(result?.content).toHaveLength(1);
    });

    it("should return empty on error", async () => {
      vi.mocked(apiClient.get).mockRejectedValue(new Error("Error"));

      const result = await feedApi.getFeedsByUserId(1);

      expect(result?.content).toEqual([]);
    });
  });

  describe("getFeedById", () => {
    it("should call GET /feeds/{feedId}", async () => {
      const mockResponse = {
        data: { id: 123, images: ["img.jpg"], writerAvatar: null },
      };
      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await feedApi.getFeedById(123);

      expect(apiClient.get).toHaveBeenCalledWith("/feeds/123");
      expect(result?.id).toBe(123);
    });

    it("should return null on error", async () => {
      vi.mocked(apiClient.get).mockRejectedValue(new Error("Not found"));

      const result = await feedApi.getFeedById(999);

      expect(result).toBeNull();
    });
  });

  describe("createFeed", () => {
    it("should call POST /feeds", async () => {
      const feedData = { activity: "running", content: "test", startImage: "start.jpg", endImage: "end.jpg" };
      const mockResponse = { data: { id: 1, ...feedData } };
      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await feedApi.createFeed(feedData);

      expect(apiClient.post).toHaveBeenCalledWith("/feeds", feedData);
      expect(result.id).toBe(1);
    });

    it("should throw on error", async () => {
      vi.mocked(apiClient.post).mockRejectedValue(new Error("Create failed"));

      await expect(feedApi.createFeed({ activity: "", content: "", startImage: "", endImage: "" })).rejects.toThrow();
    });
  });

  describe("updateFeed", () => {
    it("should call PUT /feeds/{feedId}", async () => {
      const updateData = { activity: "swimming", content: "updated" };
      const mockResponse = { data: { id: 1, ...updateData } };
      vi.mocked(apiClient.put).mockResolvedValue(mockResponse);

      const result = await feedApi.updateFeed(1, updateData);

      expect(apiClient.put).toHaveBeenCalledWith("/feeds/1", updateData);
      expect(result.activity).toBe("swimming");
    });
  });

  describe("deleteFeed", () => {
    it("should call DELETE /feeds/{feedId}", async () => {
      const mockResponse = { data: { success: true } };
      vi.mocked(apiClient.delete).mockResolvedValue(mockResponse);

      const result = await feedApi.deleteFeed(123);

      expect(apiClient.delete).toHaveBeenCalledWith("/feeds/123");
      expect(result.success).toBe(true);
    });

    it("should throw on error", async () => {
      vi.mocked(apiClient.delete).mockRejectedValue(new Error("Delete failed"));

      await expect(feedApi.deleteFeed(123)).rejects.toThrow("Delete failed");
    });
  });

  describe("likeFeed", () => {
    it("should call POST /feeds/{feedId}/like", async () => {
      const mockResponse = { data: { liked: true } };
      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await feedApi.likeFeed(123);

      expect(apiClient.post).toHaveBeenCalledWith("/feeds/123/like");
      expect(result.liked).toBe(true);
    });

    it("should throw error on failure", async () => {
      vi.mocked(apiClient.post).mockRejectedValue(new Error("Like failed"));

      await expect(feedApi.likeFeed(123)).rejects.toThrow("Like failed");
    });
  });

  describe("unlikeFeed", () => {
    it("should call DELETE /feeds/{feedId}/like", async () => {
      const mockResponse = { data: { unliked: true } };
      vi.mocked(apiClient.delete).mockResolvedValue(mockResponse);

      const result = await feedApi.unlikeFeed(123);

      expect(apiClient.delete).toHaveBeenCalledWith("/feeds/123/like");
      expect(result.unliked).toBe(true);
    });

    it("should throw error on failure", async () => {
      vi.mocked(apiClient.delete).mockRejectedValue(new Error("Unlike failed"));

      await expect(feedApi.unlikeFeed(123)).rejects.toThrow("Unlike failed");
    });
  });

  describe("canPostToday", () => {
    it("should call GET /feeds/can-post-today", async () => {
      const mockResponse = { data: true };
      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await feedApi.canPostToday();

      expect(apiClient.get).toHaveBeenCalledWith("/feeds/can-post-today");
      expect(result).toBe(true);
    });

    it("should return true on error", async () => {
      vi.mocked(apiClient.get).mockRejectedValue(new Error("Check failed"));

      const result = await feedApi.canPostToday();

      expect(result).toBe(true);
    });
  });
});
