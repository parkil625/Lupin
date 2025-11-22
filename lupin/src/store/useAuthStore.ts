/**
 * src/store/useAuthStore.ts
 */
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

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
            },

            logout: () => {
                localStorage.removeItem('accessToken');
                set({ isLoggedIn: false, userRole: 'member' });
            },
        }),
        {
            name: 'auth-storage', // localStorage에 저장될 Key 이름
            storage: createJSONStorage(() => localStorage), // 저장소 지정
        }
    )
);