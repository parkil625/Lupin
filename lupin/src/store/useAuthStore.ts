/**
 * src/store/useAuthStore.ts
 * 순환 참조 방지를 위해 apiClient 대신 fetch 직접 사용
 */
import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

const API_BASE_URL =
  import.meta.env.VITE_API_URL || "http://localhost:8081/api";

type UserRole = "member" | "doctor" | null;

interface AuthState {
  isLoggedIn: boolean;
  userRole: UserRole;
  hasHydrated: boolean;
  // Actions
  login: (token: string, role: string) => void;
  logout: () => void;
  setHasHydrated: (state: boolean) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      isLoggedIn: false,
      userRole: "member",
      hasHydrated: false,
      setHasHydrated: (state) => set({ hasHydrated: state }),

      login: (token, role) => {
        localStorage.setItem("accessToken", token);
        set({
          isLoggedIn: true,
          userRole: role === "DOCTOR" ? "doctor" : "member",
        });
      },

      logout: async () => {
        try {
          const token = localStorage.getItem("accessToken");
          if (token) {
            await fetch(`${API_BASE_URL}/auth/logout`, {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
              },
              credentials: "include",
            });
          }
        } catch {
          // 로그아웃 API 에러 무시
        } finally {
          // [수정] 모든 개인정보 관련 키 삭제 (잔존 데이터 방지)
          localStorage.removeItem("accessToken");
          localStorage.removeItem("userId");
          localStorage.removeItem("userEmail");
          localStorage.removeItem("userName");
          localStorage.removeItem("userDepartment");

          // 프로필 정보도 반드시 삭제
          localStorage.removeItem("userHeight");
          localStorage.removeItem("userWeight");
          localStorage.removeItem("userBirthDate");
          localStorage.removeItem("userGender");

          set({ isLoggedIn: false, userRole: "member" });
          window.location.href = "/login";
        }
      },
    }),
    {
      name: "auth-storage", // localStorage에 저장될 Key 이름
      storage: createJSONStorage(() => localStorage), // 저장소 지정
      // hasHydrated는 런타임 상태이므로 저장하지 않음
      partialize: (state) => ({
        isLoggedIn: state.isLoggedIn,
        userRole: state.userRole,
      }),
      onRehydrateStorage: () => (state) => {
        state?.setHasHydrated(true);
      },
    }
  )
);
