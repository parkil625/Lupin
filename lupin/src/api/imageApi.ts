import apiClient from './client';

// CDN 기본 URL (Cloudflare)
const CDN_BASE_URL = 'https://cdn.lupin-care.com';
const S3_BASE_URL = 'https://lupin-storage.s3.ap-northeast-2.amazonaws.com';

/**
 * S3 URL을 CDN URL로 변환
 * 기존 S3 URL도 CDN으로 변환하여 캐싱 혜택 받기
 */
export const getCdnUrl = (urlOrKey: string): string => {
  if (!urlOrKey) return urlOrKey;

  // 이미 CDN URL이면 그대로 반환
  if (urlOrKey.startsWith(CDN_BASE_URL)) {
    return urlOrKey;
  }

  // S3 URL이면 CDN으로 변환
  if (urlOrKey.startsWith(S3_BASE_URL)) {
    return urlOrKey.replace(S3_BASE_URL, CDN_BASE_URL);
  }

  // http로 시작하지 않으면 S3 키로 간주
  if (!urlOrKey.startsWith('http')) {
    return `${CDN_BASE_URL}/${urlOrKey}`;
  }

  return urlOrKey;
};

/**
 * 썸네일 URL 생성 (feed 이미지용)
 */
export const getThumbnailUrl = (url: string): string => {
  const cdnUrl = getCdnUrl(url);
  if (cdnUrl.includes('/feed/') && !cdnUrl.includes('/feed/thumb/')) {
    return cdnUrl.replace('/feed/', '/feed/thumb/');
  }
  return cdnUrl;
};

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
    return getCdnUrl(s3Key);
  },
};
