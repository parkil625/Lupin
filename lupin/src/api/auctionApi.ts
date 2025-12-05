import client from "@/api/client";


export const getActiveAuction = async () => {
  const response =await client.get("/auction/active");
  return response.data;

};

// export const placeBid = async (auctionId: number, amount: number) => {
//     const response = await client.post(`/auction/${auctionId}/bids`, { amount });
//     return response.data;
// };

