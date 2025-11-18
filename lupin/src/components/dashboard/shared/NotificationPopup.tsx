/**
 * NotificationPopup.tsx
 *
 * 알림 팝업 컴포넌트
 * - 사이드바에서 알림 버튼 클릭 시 표시
 * - 좋아요, 댓글, 예약, 챌린지 등 다양한 알림 타입 지원
 * - 읽지 않은 알림 강조 표시
 */

import { useEffect, useRef } from "react";
import {
  Heart,
  MessageCircle,
  Calendar as CalendarIcon,
  Zap,
} from "lucide-react";
import { Notification } from "@/types/dashboard.types";
import { ScrollArea } from "@/components/ui/scroll-area";

interface NotificationPopupProps {
  notifications: Notification[];
  onClose: (closeSidebar?: boolean) => void;
}

export default function NotificationPopup({
  notifications,
  onClose,
}: NotificationPopupProps) {
  const popupRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        popupRef.current &&
        !popupRef.current.contains(event.target as Node)
      ) {
        // 클릭된 요소가 사이드바 영역인지 확인
        const target = event.target as HTMLElement;
        const sidebar = target.closest('[data-sidebar="true"]');

        // 사이드바 클릭이면 사이드바는 유지, 아니면 사이드바도 닫기
        onClose(!sidebar);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [onClose]);

  return (
    <div
      ref={popupRef}
      className="absolute left-0 ml-20 w-80 backdrop-blur-3xl bg-white/50 border border-white/60 shadow-2xl rounded-2xl z-50 overflow-hidden"
      style={{
        height: "544px",
        maxHeight: "544px",
        bottom: "calc(100% - 32px)",
      }}
    >
      <div className="flex flex-col" style={{ height: '544px' }}>
        <div className="p-4 pb-2 flex-shrink-0">
          <h3 className="text-lg font-black text-gray-900">알림</h3>
        </div>
        <div className="flex-1 overflow-hidden px-4">
          <ScrollArea className="h-full">
            <div className="space-y-2 pb-4 pr-4">
            {notifications.map((notif) => (
              <div
                key={notif.id}
                className={`p-3 rounded-xl cursor-pointer transition-all ${
                  notif.read
                    ? "bg-white/60"
                    : "bg-gradient-to-r from-red-50/80 to-pink-50/80"
                }`}
              >
                <div className="flex items-start gap-3">
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 ${
                      notif.type === "challenge"
                        ? "bg-gradient-to-br from-purple-400 to-pink-500"
                        : notif.type === "appointment"
                        ? "bg-gradient-to-br from-blue-400 to-cyan-500"
                        : notif.type === "like"
                        ? "bg-gradient-to-br from-red-400 to-pink-500"
                        : "bg-gradient-to-br from-green-400 to-emerald-500"
                    }`}
                  >
                    {notif.type === "challenge" && (
                      <Zap className="w-4 h-4 text-white" />
                    )}
                    {notif.type === "appointment" && (
                      <CalendarIcon className="w-4 h-4 text-white" />
                    )}
                    {notif.type === "like" && (
                      <Heart className="w-4 h-4 text-white" />
                    )}
                    {notif.type === "comment" && (
                      <MessageCircle className="w-4 h-4 text-white" />
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="font-bold text-sm text-gray-900 mb-1">
                      {notif.title}
                    </div>
                    <div className="text-xs text-gray-700 mb-1 line-clamp-2">
                      {notif.content}
                    </div>
                    <div className="text-xs text-gray-500">{notif.time}</div>
                  </div>
                  {!notif.read && (
                    <div className="w-2 h-2 bg-[#C93831] rounded-full flex-shrink-0 mt-1"></div>
                  )}
                </div>
              </div>
              ))}
            </div>
          </ScrollArea>
        </div>
      </div>
    </div>
  );
}
