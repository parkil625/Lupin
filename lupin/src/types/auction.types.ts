// src/types/auction.types.ts

// 1. 백엔드의 AuctionItemResponse에 해당하는 타입
export interface AuctionItemDetail {
  itemId: number; // AuctionItemResponse에 id가 있다고 가정
  itemName: string;
  description: string;
  imageUrl?: string;
  // 필요한 경우 추가 필드
}

// 2. 백엔드의 OngoingAuctionResponse와 일치시키는 타입
export interface AuctionData {
  auctionId: number;        // 백엔드: auctionId
  status: "SCHEDULED" | "ACTIVE" | "ENDED" | "CANCELLED";
  startTime: string;        // LocalDateTime -> string
  regularEndTime: string;
  currentPrice: number;     // 백엔드: currentPrice
  overtimeStarted: boolean;
  overtimeEndTime?: string;
  overtimeSeconds: number;
  
  item: AuctionItemDetail;  
  
  // totalBids, viewers는 별도 API나 DTO에 없다면 제외하거나 optional로 변경
  totalBids?: number;
  winnerName?: string;
}

// 입찰 내역 타입 (기존 유지 혹은 백엔드 DTO에 맞춤)
export interface BidHistory {
  id: number;
  userId: number;
  userName: string;
  bidAmount: number;
  bidTime: string;
  status: "ACTIVE" | "OUTBID" | "WINNING" | "LOST";
}