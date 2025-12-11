import { useState, useEffect, useRef } from "react";
import { AuctionData } from "@/types/auction.types";

export const useAuctionTimer = (
    selectedAuction: AuctionData | null,
    refreshData?: () => Promise<void> | void
) => {
    const [countdown, setCountdown] = useState<number>(0);
    const [isOvertime, setIsOvertime] = useState(false);
    const isFetching = useRef(false);

    useEffect(() => {
        if (!selectedAuction || selectedAuction.status !== "ACTIVE") {
            setCountdown(0);
            setIsOvertime(false);
            return;
        }

        const tick = async () => {
            const now = Date.now();
            const regularEnd = new Date(selectedAuction.regularEndTime).getTime();

            // [Logic B 핵심 수정]
            // 목표 시간 설정
            let targetEndTime = 0;
            let currentIsOvertime = false;

            if (selectedAuction.overtimeStarted && selectedAuction.overtimeEndTime) {
                // 1. 서버에서 공식적으로 초읽기가 시작된 경우
                targetEndTime = new Date(selectedAuction.overtimeEndTime).getTime();
                currentIsOvertime = true;
            } else {
                // 2. 아직 초읽기 플래그는 없지만...
                if (now >= regularEnd) {
                    // 정규 시간이 지났다면 -> "자동 초읽기(30초)"로 간주!
                    // 마감 시간 = 정규 종료 + 30초
                    const overtimeSec = selectedAuction.overtimeSeconds ?? 30;
                    targetEndTime = regularEnd + (overtimeSec * 1000);
                    currentIsOvertime = true; // 화면에는 초읽기로 표시
                } else {
                    // 정규 시간 안쪽인 경우
                    targetEndTime = regularEnd;
                    currentIsOvertime = false;
                }
            }

            // 남은 시간 계산
            const diffSeconds = Math.floor((targetEndTime - now) / 1000);

            if (diffSeconds >= 0) {
                setCountdown(diffSeconds);
                setIsOvertime(currentIsOvertime);
            } else {
                // 진짜로 모든 시간이 끝남
                setCountdown(0);
                setIsOvertime(false);

                if (refreshData && !isFetching.current) {
                    isFetching.current = true;
                    try {
                        await refreshData();
                    } finally {
                        isFetching.current = false;
                    }
                }
            }
        };

        tick();
        const interval = setInterval(tick, 1000);

        return () => clearInterval(interval);
    }, [
        selectedAuction?.auctionId,
        selectedAuction?.regularEndTime,
        selectedAuction?.overtimeEndTime,
        selectedAuction?.overtimeStarted,
        selectedAuction?.status,
        refreshData,
    ]);

    return { countdown, isOvertime };
};