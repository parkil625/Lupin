import client from "@/api/client";


export const getActiveAuction = async () => {
  const response =await client.get("/auction/active");
  return response.data;

};

// 예정된 경매 (배열 반환 + 필드 채우기)
export const getScheduledAuctions = async () => { // 이름 변경: Auction -> Auctions
    const response = await client.get("/auction/scheduled");

    // 백엔드 데이터(ScheduledResponse)를 프론트엔드 타입(AuctionData)에 맞게 변환
    // response.data가 배열이라고 가정
    return response.data.map((item: any) => ({
        ...item,
        status: "SCHEDULED",    // 강제로 상태 주입
        currentPrice: 0,        // 시작 전이므로 0원 처리
        overtimeStarted: false,
        overtimeSeconds: 0
    }));
};


 export const placeBid = async (auctionId: number, amount: number) => {
     const response = await client.post(`/auction/${auctionId}/bids`, { amount });
     return response.data;
 };

