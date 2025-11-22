/**
 * src/contexts/AuthContext.tsx
 * * 모던 리액트 패턴:
 * 1. 인증 상태와 로직을 한 곳(Context)에서 관리
 * 2. 새로고침 시 localStorage를 확인하여 상태 자동 복구 (Persist)
 */
import React, {createContext, useContext, useState} from 'react';

type UserRole = 'member' | 'doctor' | null;

interface AuthContextType {
    isLoggedIn: boolean;
    userRole: UserRole;
    login: (token: string, role: string) => void;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({children}: { children: React.ReactNode }) {
    // 초기값 설정 시 localStorage를 확인하는 것이 핵심 (Lazy Initialization)
    const [isLoggedIn, setIsLoggedIn] = useState<boolean>(() => {
        return !!localStorage.getItem('accessToken');
    });

    const [userRole, setUserRole] = useState<UserRole>(() => {
        const role = localStorage.getItem('userRole');
        return role === 'DOCTOR' ? 'doctor' : 'member';
    });

    const login = (token: string, role: string) => {
        // 1. 스토리지 저장
        localStorage.setItem('accessToken', token);
        localStorage.setItem('userRole', role);

        // 2. 상태 업데이트
        setIsLoggedIn(true);
        setUserRole(role === 'DOCTOR' ? 'doctor' : 'member');
    };

    const logout = () => {
        localStorage.clear(); // 혹은 필요한 항목만 removeItem
        setIsLoggedIn(false);
        setUserRole('member');
    };

    return (
        <AuthContext.Provider value={{isLoggedIn, userRole, login, logout}}>
            {children}
        </AuthContext.Provider>
    );
}

// Custom Hook으로 쉽게 사용하도록 만듦
export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) throw new Error('useAuth must be used within an AuthProvider');
    return context;
}