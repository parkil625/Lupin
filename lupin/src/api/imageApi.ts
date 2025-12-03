import apiClient from './client';

export const imageApi = {
  uploadImage: async (file: File) => {
    const formData = new FormData();
    formData.append('image', file);
    const response = await apiClient.post('/images/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  uploadProfileImage: async (file: File) => {
    const formData = new FormData();
    formData.append('image', file);
    const response = await apiClient.post('/images/upload/profile', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  uploadFeedImage: async (file: File) => {
    const formData = new FormData();
    formData.append('image', file);
    const response = await apiClient.post('/images/upload/feed', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  uploadImages: async (files: File[]) => {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    const response = await apiClient.post('/images/upload-multiple', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  deleteImage: async (imageId: string) => {
    const response = await apiClient.delete(`/images/${imageId}`);
    return response.data;
  },

  getImageUrl: (s3Key: string) => {
    return `https://lupin-storage.s3.ap-northeast-2.amazonaws.com/${s3Key}`;
  },
};
