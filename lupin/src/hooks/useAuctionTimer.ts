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

            // 목표 시간 설정
            let targetEndTime = 0;
            let currentIsOvertime = false;

            if (selectedAuction.overtimeStarted && selectedAuction.overtimeEndTime) {
                // 1. 서버에서 공식적으로 초읽기가 시작된 경우
                targetEndTime = new Date(selectedAuction.overtimeEndTime).getTime();
                currentIsOvertime = true;
            } else {
                // 2. 정규 시간 초과 시 자동 초읽기 간주 로직
                if (now >= regularEnd) {
                    const overtimeSec = selectedAuction.overtimeSeconds ?? 30;
                    targetEndTime = regularEnd + (overtimeSec * 1000);
                    currentIsOvertime = true;
                } else {
                    targetEndTime = regularEnd;
                    currentIsOvertime = false;
                }
            }

            // 남은 시간 계산
            const diffSeconds = Math.floor((targetEndTime - now) / 1000);

            if (diffSeconds >= 0) {
                // [수정 1] 중복 제거 및 최적화 적용
                // 값이 변했을 때만 업데이트 (렌더링 최적화)
                setCountdown((prev) => {
                    if (prev !== diffSeconds) return diffSeconds;
                    return prev;
                });
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
        // [수정 2] 반응 속도 향상을 위해 1000 -> 100으로 변경
        const interval = setInterval(tick, 100);

        return () => clearInterval(interval);
    }, [
        selectedAuction?.auctionId,
        selectedAuction?.regularEndTime,
        selectedAuction?.overtimeEndTime,
        selectedAuction?.overtimeStarted,
        selectedAuction?.status,
        refreshData, // refreshData 의존성 유지
    ]);

    return { countdown, isOvertime };
};