/**
 * Sidebar.tsx
 *
 * 대시보드 공통 사이드바 컴포넌트
 * - 회원/의사 모드에서 공통으로 사용
 * - 네비게이션 메뉴 표시
 * - 마우스 호버 시 확장/축소 애니메이션
 * - 프로필 아바타 및 로고 표시
 */

import React from "react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { LucideIcon, User } from "lucide-react";

interface NavItem {
  id: string;
  icon: LucideIcon;
  label: string;
}

interface SidebarProps {
  expanded: boolean;
  onExpandChange: (expanded: boolean) => void;
  navItems: NavItem[];
  selectedNav: string;
  onNavSelect: (nav: string) => void;
  userType: "member" | "doctor";
  profileImage?: string | null;
  children?: React.ReactNode;
}

export default function Sidebar({ expanded, onExpandChange, navItems, selectedNav, onNavSelect, userType, profileImage, children }: SidebarProps) {
  return (
    <div data-sidebar="true" className={`fixed left-0 top-0 h-full z-50 transition-all duration-300 ${expanded ? 'w-64' : 'w-20'}`}
      onMouseEnter={() => onExpandChange(true)} onMouseLeave={() => onExpandChange(false)}>
      <div className="absolute inset-0 backdrop-blur-3xl bg-white/40 border-r border-white/60 shadow-2xl"></div>
      <div className="relative h-full flex flex-col p-4">
        <div className="mb-8 flex items-center justify-center">
          <img src="/Lupin.png" alt="Lupin Logo" className="h-14 w-auto object-contain" />
        </div>
        <nav className="flex-1 space-y-2">
          {navItems.map((item) => (
            <button key={item.id} onClick={() => onNavSelect(item.id)}
              className={`w-full flex items-center ${expanded ? 'gap-4' : 'justify-center'} px-0 py-3 rounded-2xl transition-all duration-200 ${selectedNav === item.id ? '' : 'hover:bg-white/30'}`}>
              <item.icon className={`w-7 h-7 flex-shrink-0 ${selectedNav === item.id ? 'text-[#C93831]' : 'text-gray-700'}`} strokeWidth={selectedNav === item.id ? 2.5 : 2} />
              <span className={`whitespace-nowrap transition-opacity duration-200 font-medium text-gray-700 ${expanded ? 'opacity-100' : 'opacity-0 w-0'}`}>{item.label}</span>
            </button>
          ))}
        </nav>
        {children}
        <button onClick={() => onNavSelect("profile")} className={`flex items-center ${expanded ? 'gap-3' : 'justify-center'} px-0 py-3 rounded-2xl hover:bg-white/30 transition-all`}>
          <Avatar className="w-9 h-9 border-2 border-[#C93831] flex-shrink-0">
            {profileImage ? <img src={profileImage} alt="Profile" className="w-full h-full object-cover" /> :
              <AvatarFallback className="bg-white">
                <User className="w-5 h-5 text-gray-400" />
              </AvatarFallback>}
          </Avatar>
          <span className={`whitespace-nowrap transition-opacity duration-200 text-sm font-medium text-gray-700 ${expanded ? 'opacity-100' : 'opacity-0 w-0'}`}>
            {userType === "doctor" ? "김의사" : "김루핀"}
          </span>
        </button>
      </div>
    </div>
  );
}
