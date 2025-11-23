import apiClient from './client';
import { LoginResponse } from './authApi';

// [확인] 이미 정의된 이 인터페이스를 사용하면 됩니다.
export interface OAuthConnection {
    id: number;
    provider: string;
    providerEmail: string | null;
    connectedAt: string;
}

const NAVER_CLIENT_ID = import.meta.env.VITE_NAVER_CLIENT_ID || '';
const KAKAO_CLIENT_ID = import.meta.env.VITE_KAKAO_CLIENT_ID || '';

/**
 * OAuth API
 */
export const oauthApi = {
    /**
     * 네이버 인가 URL 생성
     */
    getNaverAuthUrl: (redirectUri: string, state: string): string => {
        const params = new URLSearchParams({
            response_type: 'code',
            client_id: NAVER_CLIENT_ID,
            redirect_uri: redirectUri,
            state: state,
        });
        return `https://nid.naver.com/oauth2.0/authorize?${params.toString()}`;
    },

    /**
     * 네이버 로그인
     */
    naverLogin: async (code: string, state: string): Promise<LoginResponse> => {
        const response = await apiClient.post<LoginResponse>('/oauth/naver/login', {
            code,
            state,
        });
        return response.data;
    },

    /**
     * 네이버 계정 연동
     */
    linkNaver: async (code: string, state: string): Promise<OAuthConnection> => {
        const response = await apiClient.post<OAuthConnection>('/oauth/naver/link', {
            code,
            state,
        });
        return response.data;
    },

    /**
     * OAuth 연동 목록 조회
     */
    getConnections: async (): Promise<OAuthConnection[]> => {
        const response = await apiClient.get<OAuthConnection[]>('/oauth/connections');
        return response.data;
    },

    /**
     * OAuth 연동 해제
     */
    unlinkOAuth: async (provider: string): Promise<void> => {
        await apiClient.delete(`/oauth/connections/${provider}`);
    },

    /**
     * 카카오 인가 URL 생성
     */
    getKakaoAuthUrl: (redirectUri: string, state: string): string => {
        const params = new URLSearchParams({
            response_type: 'code',
            client_id: KAKAO_CLIENT_ID,
            redirect_uri: redirectUri,
            state: state,
        });
        return `https://kauth.kakao.com/oauth/authorize?${params.toString()}`;
    },

    /**
     * 카카오 로그인
     */
    kakaoLogin: async (code: string, redirectUri: string): Promise<LoginResponse> => {
        const response = await apiClient.post<LoginResponse>('/oauth/kakao/login', {
            code,
            redirectUri,
        });
        return response.data;
    },

    /**
     * 카카오 계정 연동
     */
    linkKakao: async (code: string, redirectUri: string): Promise<OAuthConnection> => {
        const response = await apiClient.post<OAuthConnection>('/oauth/kakao/link', {
            code,
            redirectUri,
        });
        return response.data;
    },

    /**
     * 구글 계정 연동
     */
    // [수정] OAuthConnectionResponse -> OAuthConnection 으로 변경
    linkGoogle: async (googleToken: string): Promise<OAuthConnection> => {
        const response = await apiClient.post<OAuthConnection>('/oauth/google/link', {
            token: googleToken,
        });
        return response.data;
    },
};