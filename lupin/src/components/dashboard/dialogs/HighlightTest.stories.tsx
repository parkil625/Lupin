/**
 * HighlightTest.stories.tsx
 *
 * 댓글 하이라이트 기능 - 실제 FeedDetailDialogHome과 동일한 구조로 테스트
 */

import type { Meta, StoryObj } from "@storybook/react-vite";
import { useState, useEffect } from "react";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { User, Heart } from "lucide-react";

// 실제 FeedDetailDialogHome과 동일한 구조로 테스트
function HighlightTestComponent() {
  const [targetId, setTargetId] = useState<number | null>(null);
  const [open, setOpen] = useState(true);
  const [showComments] = useState(true);

  // 하이라이트 로직 (FeedDetailDialogHome과 동일)
  useEffect(() => {
    if (targetId && open && showComments) {
      console.log('[Highlight] 조건 충족, targetId:', targetId);

      setTimeout(() => {
        const el = document.getElementById(`comment-${targetId}`);
        console.log('[Highlight] 요소 검색:', `comment-${targetId}`, el ? '찾음' : '없음');

        if (el) {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' });

          // 하이라이트 효과 - Tailwind 클래스 적용
          el.classList.add('bg-amber-50');

          console.log('[Highlight] 클래스 적용:', el.className);

          setTimeout(() => {
            el.classList.remove('bg-amber-50');
            console.log('[Highlight] 클래스 제거');
          }, 3000);
        }
      }, 500);
    }
  }, [targetId, open, showComments]);

  const mockComments = [
    { id: 1, author: "김철수", time: "1시간 전", content: "테스트 댓글 1번입니다. 첫 번째 댓글이에요." },
    { id: 2, author: "이영희", time: "2시간 전", content: "테스트 댓글 2번입니다." },
    { id: 3, author: "박민수", time: "3시간 전", content: "테스트 댓글 3번입니다." },
    { id: 4, author: "최지우", time: "4시간 전", content: "테스트 댓글 4번입니다." },
    { id: 5, author: "정다은", time: "5시간 전", content: "테스트 댓글 5번입니다." },
    { id: 6, author: "한소희", time: "6시간 전", content: "테스트 댓글 6번입니다." },
    { id: 7, author: "강동원", time: "7시간 전", content: "테스트 댓글 7번입니다. 중간쯤 댓글!" },
    { id: 8, author: "송혜교", time: "8시간 전", content: "테스트 댓글 8번입니다." },
    { id: 9, author: "현빈", time: "9시간 전", content: "테스트 댓글 9번입니다." },
    { id: 10, author: "손예진", time: "10시간 전", content: "테스트 댓글 10번입니다." },
    { id: 11, author: "공유", time: "11시간 전", content: "테스트 댓글 11번입니다." },
    { id: 12, author: "김태희", time: "12시간 전", content: "테스트 댓글 12번입니다." },
    { id: 13, author: "이민호", time: "13시간 전", content: "테스트 댓글 13번입니다." },
    { id: 14, author: "전지현", time: "14시간 전", content: "테스트 댓글 14번입니다." },
    { id: 15, author: "박보검", time: "15시간 전", content: "테스트 댓글 15번입니다. 마지막 댓글!" },
  ];

  return (
    <div className="p-4 bg-gray-100 min-h-screen">
      {/* 컨트롤 버튼 */}
      <div className="mb-4 flex flex-wrap gap-2">
        <button
          onClick={() => {
            setTargetId(null);
            setOpen(false);
            setTimeout(() => {
              setTargetId(1);
              setOpen(true);
            }, 100);
          }}
          className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
        >
          댓글 1 (첫번째)
        </button>
        <button
          onClick={() => {
            setTargetId(null);
            setOpen(false);
            setTimeout(() => {
              setTargetId(7);
              setOpen(true);
            }, 100);
          }}
          className="px-4 py-2 bg-orange-500 text-white rounded-lg hover:bg-orange-600"
        >
          댓글 7 (중간)
        </button>
        <button
          onClick={() => {
            setTargetId(null);
            setOpen(false);
            setTimeout(() => {
              setTargetId(15);
              setOpen(true);
            }, 100);
          }}
          className="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600"
        >
          댓글 15 (마지막)
        </button>
      </div>

      <p className="text-sm text-gray-600 mb-4">
        현재 targetId: <strong>{targetId ?? "없음"}</strong>
      </p>

      {/* 실제 FeedDetailDialogHome과 동일한 Dialog 구조 */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent
          className="p-0 w-full h-[calc(100%-70px)] max-h-[calc(100vh-70px)] md:h-[95vh] md:max-h-[95vh] overflow-hidden backdrop-blur-2xl bg-white/60 border-0 shadow-2xl md:!w-[825px] md:!max-w-[825px]"
        >
          <DialogHeader className="sr-only">
            <DialogTitle>하이라이트 테스트</DialogTitle>
            <DialogDescription>댓글 하이라이트 테스트</DialogDescription>
          </DialogHeader>

          <div className="relative h-full flex flex-col md:flex-row overflow-hidden">
            {/* 피드 콘텐츠 영역 (왼쪽) */}
            <div className="w-full md:w-[475px] md:max-w-[475px] flex-shrink-0 flex flex-col overflow-hidden bg-gray-200 items-center justify-center">
              <p className="text-gray-500">피드 콘텐츠 영역</p>
            </div>

            {/* 댓글 패널 (오른쪽) - 실제 구조와 동일 */}
            {showComments && (
              <div className="flex-1 bg-white/50 backdrop-blur-sm border-l border-gray-200/50 flex flex-col overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-200/50">
                  <h3 className="text-lg font-bold text-gray-900">댓글 {mockComments.length}개</h3>
                </div>

                {/* ScrollArea - 실제와 동일 */}
                <div className="flex-1 overflow-hidden">
                  <ScrollArea className="h-full">
                    <div className="space-y-4 px-6 pt-4 pb-4">
                      {mockComments.map((comment) => (
                        <div
                          key={comment.id}
                          id={`comment-${comment.id}`}
                          className="transition-colors duration-500"
                        >
                          <div className="flex gap-3">
                            <Avatar className="w-8 h-8 flex-shrink-0">
                              <AvatarFallback className="bg-white">
                                <User className="w-4 h-4 text-gray-400" />
                              </AvatarFallback>
                            </Avatar>
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center gap-2 mb-1">
                                <span className="font-bold text-sm text-gray-900">{comment.author}</span>
                                <span className="text-xs text-gray-500">{comment.time}</span>
                              </div>
                              <p className="text-sm text-gray-900 break-words mb-2">
                                {comment.content}
                              </p>
                              <div className="flex items-center gap-4 mb-2">
                                <button className="flex items-center gap-1 hover:opacity-70 transition-opacity cursor-pointer">
                                  <Heart className="w-4 h-4 text-gray-600" />
                                </button>
                                <button className="text-xs text-gray-600 hover:text-[#C93831] font-semibold cursor-pointer">
                                  답글
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </ScrollArea>
                </div>
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* 디버그 정보 */}
      <div className="mt-4 p-4 bg-gray-800 text-green-400 rounded-lg font-mono text-sm">
        <p>콘솔(F12)을 열어서 [Highlight] 로그를 확인하세요.</p>
        <p>버튼 클릭 시 해당 댓글에 배경색(bg-amber-50)이 3초간 표시되어야 합니다.</p>
        <p className="text-yellow-400 mt-2">이 구조는 실제 FeedDetailDialogHome과 동일합니다 (Dialog + ScrollArea)</p>
      </div>
    </div>
  );
}

const meta: Meta = {
  title: "Dashboard/Dialogs/HighlightTest",
  component: HighlightTestComponent,
  parameters: {
    layout: "fullscreen",
  },
};

export default meta;
type Story = StoryObj;

export const Default: Story = {
  render: () => <HighlightTestComponent />,
};
