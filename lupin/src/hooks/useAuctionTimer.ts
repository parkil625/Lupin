import { useState, useEffect } from "react";
import { AuctionItem } from "@/types/auction.types";

export const useAuctionTimer = (selectedAuction: AuctionItem | null) => {
  const [countdown, setCountdown] = useState<number>(0);
  const [isOvertime, setIsOvertime] = useState(false);

  useEffect(() => {
    if (!selectedAuction || selectedAuction.status !== "ACTIVE") {
      setCountdown(0);
      setIsOvertime(false);
      return;
    }

    const calculateCountdown = () => {
      const now = Date.now();
      const regularEnd = new Date(selectedAuction.regularEndTime).getTime();

      // 정규 시간
      if (now < regularEnd) {
        setIsOvertime(false);
        const diff = Math.floor((regularEnd - now) / 1000);
        return Math.max(0, diff);
      }

      // 초읽기 모드
      if (selectedAuction.overtimeStarted && selectedAuction.overtimeEndTime) {
        setIsOvertime(true);
        const overtimeEnd = new Date(selectedAuction.overtimeEndTime).getTime();
        const diff = Math.floor((overtimeEnd - now) / 1000);
        return Math.max(0, diff);
      }

      // 정규 시간 종료 + 초읽기 미시작
      return 0;
    };

    // 초기값 설정
    setCountdown(calculateCountdown());

    const interval = setInterval(() => {
      const remaining = calculateCountdown();
      setCountdown(remaining);

      if (remaining <= 0) {
        clearInterval(interval);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [selectedAuction]);

  return { countdown, isOvertime };
};