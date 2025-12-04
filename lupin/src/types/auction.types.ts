// src/types/auction.types.ts
export interface AuctionItem {
  id: number;
  itemName: string;
  description: string;
  imageUrl?: string;
  currentPrice: number;
  startTime: string;
  regularEndTime: string;
  overtimeStarted: boolean;
  overtimeEndTime?: string;
  overtimeSeconds: number;
  status: "SCHEDULED" | "ACTIVE" | "ENDED" | "CANCELLED";
  winnerId?: number;
  totalBids: number;
  viewers: number;
}

export interface BidHistory {
  id: number;
  userId: number;
  userName: string;
  bidAmount: number;
  bidTime: string;
  status: "ACTIVE" | "OUTBID" | "WINNING" | "LOST";
}