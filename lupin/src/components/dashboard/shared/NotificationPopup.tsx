/**
 * NotificationPopup.tsx
 *
 * 알림 팝업 컴포넌트
 * - 사이드바에서 알림 버튼 클릭 시 표시
 * - 좋아요, 댓글, 예약 등 다양한 알림 타입 지원
 * - 읽지 않은 알림 강조 표시
 */

import { useEffect, useRef, useState } from "react";
import { CheckCheck, User } from "lucide-react";
import { Notification } from "@/types/dashboard.types";
import { ScrollArea } from "@/components/ui/scroll-area";
import { getRelativeTime } from "@/lib/utils";

// [수정] 알림 아이콘 결정 컴포넌트 (DB 데이터 기반 + 에러 처리 강화)
const NotificationIcon = ({ notification }: { notification: Notification }) => {
  const { type, actorProfileImage } = notification;
  const [hasError, setHasError] = useState(false);

  // 시스템 알림인지 확인 (시스템 알림은 아이콘 원본 비율 유지, 유저는 원형 크롭)
  const isSystemNotification = [
    "FEED_DELETED",
    "COMMENT_DELETED",
    "REPORT",
    "PENALTY",
    "SANCTION", // 신고
    "APPOINTMENT",
    "PRESCRIPTION",
    "MEDICINE",
    "CHAT",
    "RESERVATION",
    "DOCTOR", // 의료
    "AUCTION",
    "BID",
    "WIN", // 경매
  ].some((keyword) => type.includes(keyword));

  // DB에 이미지가 있고, 로드 에러가 안 났을 때 표시
  if (actorProfileImage && actorProfileImage.trim() !== "" && !hasError) {
    return (
      <img
        src={actorProfileImage}
        alt="알림 아이콘"
        // [수정] 시스템 알림도 rounded-full(원형) 적용 + 배경색 추가
        className={`w-10 h-10 flex-shrink-0 rounded-full ${
          isSystemNotification
            ? "object-contain bg-gray-50 border border-gray-100 p-1" // 시스템: 비율 유지, 여백, 배경
            : "object-cover" // 유저: 꽉 채우기
        }`}
        onError={() => setHasError(true)} // 이미지 로드 실패 시 에러 상태 true
      />
    );
  }

  // 이미지가 없거나 로드 실패(엑박) 시 기본 아이콘 표시
  return (
    <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center flex-shrink-0 fallback-avatar">
      <User className="w-5 h-5 text-gray-400" />
    </div>
  );
};

interface NotificationPopupProps {
  notifications: Notification[];
  onClose: (closeSidebar?: boolean) => void;
  onNotificationClick: (notification: Notification) => void;
  onMarkAllAsRead: () => void;
}

export default function NotificationPopup({
  notifications,
  onClose,
  onNotificationClick,
  onMarkAllAsRead,
}: NotificationPopupProps) {
  const mobilePopupRef = useRef<HTMLDivElement>(null);
  const desktopPopupRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Node;
      const isInsideMobile = mobilePopupRef.current?.contains(target);
      const isInsideDesktop = desktopPopupRef.current?.contains(target);

      if (!isInsideMobile && !isInsideDesktop) {
        // 클릭된 요소가 사이드바 영역인지 확인
        const targetElement = event.target as HTMLElement;
        const sidebar = targetElement.closest('[data-sidebar="true"]');

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
    <>
      {/* 모바일용 전체 화면 알림 (하단 네비 제외) */}
      <div
        ref={mobilePopupRef}
        onMouseDown={(e) => e.stopPropagation()}
        className="md:hidden fixed inset-x-0 top-0 bottom-[60px] z-40 bg-white flex flex-col"
      >
        <div className="p-4 pb-2 flex-shrink-0 flex items-center justify-between border-b">
          <h3 className="text-lg font-black text-gray-900">알림</h3>
          <div className="flex items-center gap-3">
            <button
              onMouseDown={(e) => e.stopPropagation()}
              onClick={(e) => {
                e.stopPropagation();
                onMarkAllAsRead();
              }}
              className="flex items-center gap-1 text-xs text-gray-500 hover:text-gray-700 transition-colors cursor-pointer"
            >
              <CheckCheck className="w-4 h-4" />
              모두 읽음
            </button>
            <button
              onClick={() => onClose(true)}
              className="p-2 hover:bg-gray-100 rounded-full cursor-pointer"
            >
              <span className="text-xl">×</span>
            </button>
          </div>
        </div>
        <div className="flex-1 overflow-hidden px-4">
          <ScrollArea className="h-full">
            <div className="space-y-2 pb-20 pt-4 pr-4">
              {notifications.map((notif) => (
                <div
                  key={notif.id}
                  onClick={() => {
                    onNotificationClick(notif);
                    onClose(true);
                  }}
                  className={`p-3 rounded-xl cursor-pointer transition-all hover:shadow-md ${
                    notif.isRead
                      ? "bg-gray-50"
                      : "bg-gradient-to-r from-red-50/80 to-pink-50/80"
                  }`}
                >
                  <div className="flex items-start gap-3">
                    {/* [수정] NotificationIcon 컴포넌트 사용 */}
                    <NotificationIcon notification={notif} />
                    <div className="flex-1 min-w-0">
                      <div className="font-bold text-sm text-gray-900 mb-1">
                        {notif.title}
                      </div>
                      {notif.content && (
                        <div className="text-xs text-gray-700 mb-1 line-clamp-2">
                          &ldquo;{notif.content}&rdquo;
                        </div>
                      )}
                      <div className="text-xs text-gray-500">
                        {getRelativeTime(notif.createdAt)}
                      </div>
                    </div>
                    {!notif.isRead && (
                      <div className="w-2 h-2 bg-[#C93831] rounded-full flex-shrink-0 mt-1"></div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </ScrollArea>
        </div>
      </div>

      {/* 데스크톱용 팝업 */}
      <div
        ref={desktopPopupRef}
        onMouseDown={(e) => e.stopPropagation()}
        className="hidden md:block absolute left-0 ml-20 w-80 backdrop-blur-3xl bg-white/50 border border-white/60 shadow-2xl rounded-2xl z-50 overflow-hidden"
        style={{
          height: "544px",
          maxHeight: "544px",
          bottom: "calc(100% - 32px)",
        }}
      >
        <div className="flex flex-col" style={{ height: "544px" }}>
          <div className="p-4 pb-2 flex-shrink-0 flex items-center justify-between">
            <h3 className="text-lg font-black text-gray-900">알림</h3>
            <button
              onMouseDown={(e) => e.stopPropagation()}
              onClick={(e) => {
                e.stopPropagation();
                onMarkAllAsRead();
              }}
              className="flex items-center gap-1 text-xs text-gray-500 hover:text-gray-700 transition-colors cursor-pointer"
            >
              <CheckCheck className="w-4 h-4" />
              모두 읽음
            </button>
          </div>
          <div className="flex-1 overflow-hidden px-4">
            <ScrollArea className="h-full">
              <div className="space-y-2 pb-4 pr-4">
                {notifications.map((notif) => (
                  <div
                    key={notif.id}
                    onClick={() => {
                      onNotificationClick(notif);
                      onClose(true);
                    }}
                    className={`p-3 rounded-xl cursor-pointer transition-all hover:shadow-md ${
                      notif.isRead
                        ? "bg-white/60"
                        : "bg-gradient-to-r from-red-50/80 to-pink-50/80"
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      {/* [수정] NotificationIcon 컴포넌트 사용 */}
                      <NotificationIcon notification={notif} />
                      <div className="flex-1 min-w-0">
                        <div className="font-bold text-sm text-gray-900 mb-1">
                          {notif.title}
                        </div>
                        {notif.content && (
                          <div className="text-xs text-gray-700 mb-1 line-clamp-2">
                            &ldquo;{notif.content}&rdquo;
                          </div>
                        )}
                        <div className="text-xs text-gray-500">
                          {getRelativeTime(notif.createdAt)}
                        </div>
                      </div>
                      {!notif.isRead && (
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
    </>
  );
}
