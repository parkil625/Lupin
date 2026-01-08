// src/components/dashboard/auction/AuctionTimer.tsx

import { Clock } from "lucide-react";
import { useAuctionTimer } from "@/hooks/useAuctionTimer";
import { AuctionData } from "@/types/auction.types";

interface AuctionTimerProps {
    auction: AuctionData;
    onTimeEnd?: () => void;
}

export const AuctionTimer = ({ auction, onTimeEnd }: AuctionTimerProps) => {
    const { countdown, isOvertime } = useAuctionTimer(auction, onTimeEnd);

    // [수정] 시간을 hh:mm:ss 형식으로 예쁘게 바꿔주는 함수
    const formatCountdown = (totalSeconds: number) => {
        // 시간이 음수가 되면 00:00:00으로 보여줍니다.
        if (totalSeconds < 0) return "00:00:00";

        // 1. 시간 계산: 전체 초를 3600(1시간)으로 나눈 몫
        const hours = Math.floor(totalSeconds / 3600);

        // 2. 분 계산: 시간을 빼고 남은 초(totalSeconds % 3600)를 60으로 나눈 몫
        const minutes = Math.floor((totalSeconds % 3600) / 60);

        // 3. 초 계산: 60으로 나눈 나머지
        const seconds = totalSeconds % 60;

        // 4. 두 자리수 맞추기 (0 -> 00, 9 -> 09)
        const hh = String(hours).padStart(2, "0");
        const mm = String(minutes).padStart(2, "0");
        const ss = String(seconds).padStart(2, "0");

        return `${hh}:${mm}:${ss}`;
    };

    return (
        <div
            className={`flex items-center gap-1 text-sm font-bold ${
                isOvertime ? "text-red-600" : "text-gray-600"
            }`}
        >
            <Clock className="w-4 h-4" />
            {/* 초읽기 때는 그대로 초만 보여주고, 정규 시간일 때만 포맷팅 적용 */}
            {isOvertime ? `초읽기 ${countdown}초` : formatCountdown(countdown)}
        </div>
    );
};