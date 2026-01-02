/**
 * NotificationPopup.tsx
 *
 * 알림 팝업 컴포넌트
 * - 사이드바에서 알림 버튼 클릭 시 표시
 * - 좋아요, 댓글, 예약 등 다양한 알림 타입 지원
 * - 읽지 않은 알림 강조 표시
 */

import { useEffect, useRef } from "react";
import { CheckCheck, User } from "lucide-react";
import { Notification } from "@/types/dashboard.types";
import { ScrollArea } from "@/components/ui/scroll-area";
import { getRelativeTime } from "@/lib/utils";

// [추가] 알림 아이콘 결정 컴포넌트
const NotificationIcon = ({ notification }: { notification: Notification }) => {
  const { type, actorProfileImage } = notification;

  // 디버깅용 로그
  console.log(
    `[NotificationIcon] id=${notification.id}, type=${type}, img=${actorProfileImage}`
  );

  // 1. 신고/제재 관련 (사이렌) - FEED_DELETED, COMMENT_DELETED 포함
  if (
    ["FEED_DELETED", "COMMENT_DELETED", "REPORT", "PENALTY", "SANCTION"].some(
      (keyword) => type.includes(keyword)
    )
  ) {
    console.log(" -> 신고(사이렌) 아이콘 적용");
    return (
      <img
        src="/icon-report.webp"
        alt="신고 알림"
        className="w-10 h-10 object-contain flex-shrink-0"
      />
    );
  }

  // 2. 의료/진료/채팅/예약 관련 (알약) - APPOINTMENT, PRESCRIPTION 등
  if (
    [
      "APPOINTMENT",
      "PRESCRIPTION",
      "MEDICINE",
      "CHAT",
      "RESERVATION",
      "DOCTOR",
    ].some((keyword) => type.includes(keyword))
  ) {
    console.log(" -> 의료(알약) 아이콘 적용");
    return (
      <img
        src="/icon-medicine.webp"
        alt="의료 알림"
        className="w-10 h-10 object-contain flex-shrink-0"
      />
    );
  }

  // 3. 경매 관련 (망치) - AUCTION, BID, WIN
  if (["AUCTION", "BID", "WIN"].some((keyword) => type.includes(keyword))) {
    console.log(" -> 경매(망치) 아이콘 적용");
    return (
      <img
        src="/icon-auction.webp"
        alt="경매 알림"
        className="w-10 h-10 object-contain flex-shrink-0"
      />
    );
  }

  // 4. 일반 활동 (기본 프로필 이미지 또는 기본 아이콘)
  console.log(" -> 일반 유저 프로필 적용");
  if (actorProfileImage && actorProfileImage.trim() !== "") {
    return (
      <img
        src={actorProfileImage}
        alt="프로필"
        className="w-10 h-10 rounded-full object-cover flex-shrink-0"
        onError={(e) => {
          const target = e.target as HTMLImageElement;
          target.style.display = "none";
          target.parentElement
            ?.querySelector(".fallback-avatar")
            ?.classList.remove("hidden");
        }}
      />
    );
  }

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
