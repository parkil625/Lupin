import { describe, it, expect, vi, beforeEach } from "vitest";
import { commentApi } from "./commentApi";
import apiClient from "./client";

vi.mock("./client", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("commentApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("getCommentById", () => {
    it("should call GET /comments/{commentId}", async () => {
      const mockResponse = { data: { id: 1, content: "test comment" } };
      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await commentApi.getCommentById(1);

      expect(apiClient.get).toHaveBeenCalledWith("/comments/1");
      expect(result.id).toBe(1);
    });

    it("should return null on error", async () => {
      vi.mocked(apiClient.get).mockRejectedValue(new Error("Not found"));

      const result = await commentApi.getCommentById(999);

      expect(result).toBeNull();
    });
  });

  describe("getComments", () => {
    it("should call GET /feeds/{feedId}/comments", async () => {
      const mockResponse = { data: [{ id: 1, content: "comment" }] };
      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await commentApi.getComments(123);

      expect(apiClient.get).toHaveBeenCalledWith("/feeds/123/comments");
      expect(result).toHaveLength(1);
    });

    it("should return empty array on error", async () => {
      vi.mocked(apiClient.get).mockRejectedValue(new Error("Error"));

      const result = await commentApi.getComments(123);

      expect(result).toEqual([]);
    });
  });

  describe("getCommentsByFeedId", () => {
    it("should call GET /feeds/{feedId}/comments with pagination", async () => {
      const mockResponse = {
        data: { content: [{ id: 1 }], totalElements: 1, totalPages: 1 },
      };
      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await commentApi.getCommentsByFeedId(123, 0, 10);

      expect(apiClient.get).toHaveBeenCalledWith("/feeds/123/comments?page=0&size=10");
      expect(result.content).toHaveLength(1);
    });

    it("should return empty on error", async () => {
      vi.mocked(apiClient.get).mockRejectedValue(new Error("Error"));

      const result = await commentApi.getCommentsByFeedId(123);

      expect(result.content).toEqual([]);
    });
  });

  describe("getRepliesByCommentId", () => {
    it("should call GET /comments/{commentId}/replies", async () => {
      const mockResponse = { data: [{ id: 2, parentId: 1 }] };
      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await commentApi.getRepliesByCommentId(1);

      expect(apiClient.get).toHaveBeenCalledWith("/comments/1/replies");
      expect(result).toHaveLength(1);
    });

    it("should return empty array on error", async () => {
      vi.mocked(apiClient.get).mockRejectedValue(new Error("Error"));

      const result = await commentApi.getRepliesByCommentId(1);

      expect(result).toEqual([]);
    });
  });

  describe("createComment", () => {
    it("should call POST /feeds/{feedId}/comments", async () => {
      const commentData = { content: "new comment", feedId: 123, writerId: 1 };
      const mockResponse = { data: { id: 1, ...commentData } };
      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await commentApi.createComment(commentData);

      expect(apiClient.post).toHaveBeenCalledWith("/feeds/123/comments", commentData);
      expect(result.id).toBe(1);
    });

    it("should return fallback on error", async () => {
      const commentData = { content: "new comment", feedId: 123, writerId: 1 };
      vi.mocked(apiClient.post).mockRejectedValue(new Error("Error"));

      const result = await commentApi.createComment(commentData);

      expect(result.content).toBe("new comment");
      expect(result.writerName).toBe("사용자");
    });
  });

  describe("updateComment", () => {
    it("should call PUT /comments/{commentId}", async () => {
      const mockResponse = { data: { id: 1, content: "updated" } };
      vi.mocked(apiClient.put).mockResolvedValue(mockResponse);

      const result = await commentApi.updateComment(1, "updated");

      expect(apiClient.put).toHaveBeenCalledWith("/comments/1", { content: "updated" });
      expect(result.content).toBe("updated");
    });

    it("should return fallback on error", async () => {
      vi.mocked(apiClient.put).mockRejectedValue(new Error("Error"));

      const result = await commentApi.updateComment(1, "updated");

      expect(result).toEqual({ id: 1, content: "updated" });
    });
  });

  describe("deleteComment", () => {
    it("should call DELETE /comments/{commentId}", async () => {
      const mockResponse = { data: { success: true } };
      vi.mocked(apiClient.delete).mockResolvedValue(mockResponse);

      const result = await commentApi.deleteComment(1);

      expect(apiClient.delete).toHaveBeenCalledWith("/comments/1");
      expect(result.success).toBe(true);
    });

    it("should return success on error", async () => {
      vi.mocked(apiClient.delete).mockRejectedValue(new Error("Error"));

      const result = await commentApi.deleteComment(1);

      expect(result).toEqual({ success: true });
    });
  });

  describe("likeComment", () => {
    it("should call POST /comments/{commentId}/like", async () => {
      const mockResponse = { data: { liked: true } };
      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await commentApi.likeComment(1);

      expect(apiClient.post).toHaveBeenCalledWith("/comments/1/like");
      expect(result.liked).toBe(true);
    });

    it("should return success on error", async () => {
      vi.mocked(apiClient.post).mockRejectedValue(new Error("Error"));

      const result = await commentApi.likeComment(1);

      expect(result).toEqual({ success: true });
    });
  });

  describe("unlikeComment", () => {
    it("should call DELETE /comments/{commentId}/like", async () => {
      const mockResponse = { data: { unliked: true } };
      vi.mocked(apiClient.delete).mockResolvedValue(mockResponse);

      const result = await commentApi.unlikeComment(1);

      expect(apiClient.delete).toHaveBeenCalledWith("/comments/1/like");
      expect(result.unliked).toBe(true);
    });

    it("should return success on error", async () => {
      vi.mocked(apiClient.delete).mockRejectedValue(new Error("Error"));

      const result = await commentApi.unlikeComment(1);

      expect(result).toEqual({ success: true });
    });
  });

  describe("reportComment", () => {
    it("should call POST /comments/{commentId}/report", async () => {
      const mockResponse = { data: { reported: true } };
      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await commentApi.reportComment(1);

      expect(apiClient.post).toHaveBeenCalledWith("/comments/1/report");
      expect(result.reported).toBe(true);
    });

    it("should return success on error", async () => {
      vi.mocked(apiClient.post).mockRejectedValue(new Error("Error"));

      const result = await commentApi.reportComment(1);

      expect(result).toEqual({ success: true });
    });
  });
});
