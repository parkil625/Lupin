/**
 * src/store/useAuthStore.ts
 */
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import apiClient from '../api/client';

type UserRole = 'member' | 'doctor' | null;

interface AuthState {
    isLoggedIn: boolean;
    userRole: UserRole;
    // Actions
    login: (token: string, role: string) => void;
    logout: () => void;
}

export const useAuthStore = create<AuthState>()(
    persist(
        (set) => ({
            isLoggedIn: false,
            userRole: 'member',

            login: (token, role) => {
                localStorage.setItem('accessToken', token);
                set({
                    isLoggedIn: true,
                    userRole: role === 'DOCTOR' ? 'doctor' : 'member'
                });

                // 시연용 테스트 URL 출력
                console.log(
                    '%c🎬 시연용 테스트 URL',
                    'color: #C93831; font-size: 16px; font-weight: bold;'
                );
                console.log(
                    '%c┌─────────────────────────────────────────┐\n' +
                    '│  404 페이지: /demo/404                  │\n' +
                    '│  500 페이지: /demo/500                  │\n' +
                    '└─────────────────────────────────────────┘',
                    'color: #666; font-family: monospace;'
                );
            },

            logout: async () => {
                try {
                    const token = localStorage.getItem('accessToken');
                    if (token) {
                        await apiClient.post('/auth/logout', null, {
                            headers: { Authorization: `Bearer ${token}` }
                        });
                    }
                } catch (error) {
                    console.error('Logout API error:', error);
                } finally {
                    localStorage.removeItem('accessToken');
                    localStorage.removeItem('userId');
                    localStorage.removeItem('userEmail');
                    localStorage.removeItem('userName');
                    set({ isLoggedIn: false, userRole: 'member' });
                }
            },
        }),
        {
            name: 'auth-storage', // localStorage에 저장될 Key 이름
            storage: createJSONStorage(() => localStorage), // 저장소 지정
        }
    )
);