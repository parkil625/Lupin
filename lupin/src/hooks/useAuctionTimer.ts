import {useState, useEffect, useRef} from "react";
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

        const regularEnd = new Date(selectedAuction.regularEndTime).getTime();
        const overtimeSec = selectedAuction.overtimeSeconds ?? 30;
        const overtimeEnd = regularEnd + overtimeSec * 1000;

        const tick = async () => {
            const now = Date.now();

            // 1) 정규 시간
            if (now < regularEnd) {
                setIsOvertime(false);
                setCountdown(Math.max(0, Math.floor((regularEnd - now) / 1000)));
                return;
            }

            // 2) 정규 끝난 뒤 ~ 30초 초읽기
            if (now >= regularEnd && now < overtimeEnd) {
                setIsOvertime(true);
                setCountdown(Math.max(0, Math.floor((overtimeEnd - now) / 1000)));
                return;
            }

            // 3) 완전 종료
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
        };

        // 즉시 한 번 호출
        tick();
        const interval = setInterval(tick, 1000);

        return () => clearInterval(interval);
    }, [
        selectedAuction?.auctionId,
        selectedAuction?.regularEndTime,
        selectedAuction?.overtimeSeconds,
        selectedAuction?.status,
        refreshData,
    ]);

    return { countdown, isOvertime };
};
