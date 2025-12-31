// src/components/dashboard/auction/AuctionTimer.tsx

import { Clock } from "lucide-react";
import { useAuctionTimer } from "@/hooks/useAuctionTimer";
import { AuctionData } from "@/types/auction.types";

interface AuctionTimerProps {
    auction: AuctionData;
    onTimeEnd?: () => void;
}

export const AuctionTimer = ({ auction, onTimeEnd }: AuctionTimerProps) => {
    // 여기서만 훅을 사용하므로, 이 컴포넌트만 1초마다 리렌더링 됩니다.
    const { countdown, isOvertime } = useAuctionTimer(auction, onTimeEnd);

    // 카운트다운 포맷 함수 (기존 AuctionCard에서 가져옴)
    const formatCountdown = (seconds: number) => {
        if (seconds >= 60) {
            const mins = Math.floor(seconds / 60);
            const secs = seconds % 60;
            return `${mins}:${String(secs).padStart(2, "0")}`;
        }
        return `${seconds}초`;
    };

    return (
        <div
            className={`flex items-center gap-1 text-sm font-bold ${
                isOvertime ? "text-red-600" : "text-gray-600"
            }`}
        >
            <Clock className="w-4 h-4" />
            {isOvertime ? `초읽기 ${countdown}초` : formatCountdown(countdown)}
        </div>
    );
};