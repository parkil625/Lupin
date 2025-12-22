import client from "@/api/client";
import { AuctionData, AuctionItemDetail } from "@/types/auction.types";

// 백엔드 ScheduledResponse 타입
interface ScheduledAuctionResponse {
  auctionId: number;
  startTime: string;
  regularEndTime: string;
  item: AuctionItemDetail;
}

export const getActiveAuction = async () => {
    const response = await client.get(`/auction/active?_t=${Date.now()}`);
    return response.data;
};

// 예정된 경매 (배열 반환 + 필드 채우기)
export const getScheduledAuctions = async (): Promise<AuctionData[]> => {
    const response = await client.get("/auction/scheduled");

    // 백엔드 데이터(ScheduledResponse)를 프론트엔드 타입(AuctionData)에 맞게 변환
    return response.data.map((item: ScheduledAuctionResponse) => ({
        ...item,
        status: "SCHEDULED" as const,    // 강제로 상태 주입
        currentPrice: 0,        // 시작 전이므로 0원 처리
        overtimeStarted: false,
        overtimeSeconds: 0
    }));
};


export const placeBid = async (auctionId: number, amount: number) => {
    const response = await client.post(`/auction/${auctionId}/bid`, {
        bidAmount: amount,
    });
    return response.data;
};

export const getUserPoints = async () => {
    const response = await client.get('/users/points');
    return response.data;
}

export const getBidHistory= async () => {
    const response = await client.get('/auction/active/history')
    return response.data;
}


export const getEndedAuctions = async (): Promise<AuctionData[]> => {
    const response = await client.get<AuctionData[]>('/auction/winners/monthly');
    return response.data;
};