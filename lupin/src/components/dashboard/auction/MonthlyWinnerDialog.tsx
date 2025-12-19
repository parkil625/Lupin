import { useState } from "react";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Trophy, Crown, Calendar } from "lucide-react";
import { getMonthlyWinners } from "@/api/auctionApi";
import { AuctionData } from "@/types/auction.types";

export function MonthlyWinnerDialog() {
    const [winners, setWinners] = useState<AuctionData[]>([]);

    // 팝업이 열릴 때 데이터를 서버에서 가져옵니다 (Lazy Loading)
    const handleOpenChange = async (isOpen: boolean) => {
        if (isOpen) {
            try {
                const data = await getMonthlyWinners();
                setWinners(data);
            } catch (error) {
                console.error("명예의 전당 로딩 실패:", error);
            }
        }
    };

    return (
        <Dialog onOpenChange={handleOpenChange}>
            <DialogTrigger asChild>
                {/* 버튼 디자인 */}
                <button className="flex flex-col items-center justify-center bg-yellow-400 hover:bg-yellow-500 text-white p-4 rounded-xl shadow-lg transition-all transform hover:scale-105">
                    <Trophy className="w-8 h-8 mb-1 text-white drop-shadow-md" />
                    <span className="font-bold text-sm text-shadow">이달의 왕</span>
                </button>
            </DialogTrigger>

            <DialogContent className="sm:max-w-md bg-white/95 backdrop-blur">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2 text-2xl font-black text-yellow-600">
                        <Crown className="w-6 h-6 fill-yellow-500" />
                        이달의 명예의 전당
                    </DialogTitle>
                </DialogHeader>

                <ScrollArea className="h-[400px] mt-4 pr-4">
                    {winners.length > 0 ? (
                        <div className="space-y-3">
                            {winners.map((winner, index) => (
                                <div
                                    key={winner.auctionId}
                                    className="flex items-center justify-between p-3 bg-gray-50 rounded-lg border border-gray-100 shadow-sm"
                                >
                                    <div className="flex items-center gap-3">
                                        {/* 1, 2, 3등 메달 색상 다르게 표시 */}
                                        <div
                                            className={`
                        w-8 h-8 rounded-full flex items-center justify-center font-black text-white
                        ${
                                                index === 0
                                                    ? "bg-yellow-500"
                                                    : index === 1
                                                        ? "bg-gray-400"
                                                        : index === 2
                                                            ? "bg-orange-400"
                                                            : "bg-blue-200"}`}>
                                            {index + 1}
                                        </div>
                                        <div>
                                            <p className="font-bold text-gray-800">
                                                {winner.winnerName || "알 수 없음"}
                                            </p>
                                            <p className="text-xs text-gray-500">
                                                {winner.item.itemName}
                                            </p>
                                        </div>
                                    </div>
                                    <div className="text-right">
                                        <p className="font-black text-blue-600">
                                            {winner.currentPrice.toLocaleString()}P
                                        </p>
                                        <p className="text-[10px] text-gray-400 flex items-center justify-end gap-1">
                                            <Calendar className="w-3 h-3" />
                                            {new Date(
                                                winner.overtimeEndTime || winner.regularEndTime
                                            ).toLocaleDateString()}
                                        </p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-10 text-gray-500">
                            <Trophy className="w-12 h-12 mx-auto mb-2 opacity-20" />
                            <p>
                                아직 이번 달 우승자가 없습니다.
                                <br />
                                첫 번째 주인공이 되어보세요!
                            </p>
                        </div>
                    )}
                </ScrollArea>
            </DialogContent>
        </Dialog>
    );
}