import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { reportApi } from "./reportApi";
import apiClient from "./client";

vi.mock("./client", () => ({
  default: {
    post: vi.fn(),
  },
}));

describe("reportApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("reportFeed", () => {
    it("should call POST /feeds/{feedId}/report", async () => {
      const mockResponse = { data: { success: true } };
      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await reportApi.reportFeed(123);

      expect(apiClient.post).toHaveBeenCalledWith("/feeds/123/report");
      expect(result).toEqual({ success: true });
    });

    it("should throw error on API failure", async () => {
      const error = new Error("Network error");
      vi.mocked(apiClient.post).mockRejectedValue(error);

      await expect(reportApi.reportFeed(123)).rejects.toThrow("Network error");
    });
  });

  describe("reportComment", () => {
    it("should call POST /comments/{commentId}/report", async () => {
      const mockResponse = { data: { success: true } };
      vi.mocked(apiClient.post).mockResolvedValue(mockResponse);

      const result = await reportApi.reportComment(456);

      expect(apiClient.post).toHaveBeenCalledWith("/comments/456/report");
      expect(result).toEqual({ success: true });
    });

    it("should throw error on API failure", async () => {
      const error = new Error("Network error");
      vi.mocked(apiClient.post).mockRejectedValue(error);

      await expect(reportApi.reportComment(456)).rejects.toThrow("Network error");
    });
  });
});
