import apiClient from './client';

export const imageApi = {
  /**
   * 단일 이미지 업로드
   * @param file - 업로드할 파일
   * @param type - "feed" 또는 "profile" (기본값: feed)
   */
  uploadImage: async (file: File, type: 'feed' | 'profile' = 'feed'): Promise<string> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post(`/images/upload?type=${type}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return response.data;
  },

  /**
   * 다중 이미지 업로드
   */
  uploadMultipleImages: async (files: File[]): Promise<string[]> => {
    const formData = new FormData();
    files.forEach(file => {
      formData.append('files', file);
    });

    const response = await apiClient.post('/images/upload/multiple', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return response.data;
  },

  /**
   * 이미지 삭제
   * @param url - 삭제할 S3 URL
   */
  deleteImage: async (url: string): Promise<void> => {
    await apiClient.delete('/images/delete', {
      params: { url }
    });
  },
};
