/**
 * MembersPage.tsx
 *
 * 회원 목록 페이지 컴포넌트 (의사용)
 * - 담당 회원 목록 조회
 * - 회원별 상태 및 점수 표시
 * - 회원 상세 정보 조회
 */

import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { User, Calendar as CalendarIcon, Stethoscope } from "lucide-react";
import { Member } from "@/types/dashboard.types";
import { members } from "@/mockdata/members";

interface MembersPageProps {
  onMemberSelect: (member: Member) => void;
}

export default function MembersPage({ onMemberSelect }: MembersPageProps) {
  return (
    <div className="h-full overflow-auto p-8">
      <div className="max-w-7xl mx-auto space-y-6">
        <div>
          <h1 className="text-5xl font-black text-gray-900 mb-2">회원 목록</h1>
          <p className="text-gray-700 font-medium text-lg">오늘의 진료 회원</p>
        </div>

        <div className="grid gap-4">
          {members.map((member) => (
            <Card
              key={member.id}
              className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl hover:shadow-2xl transition-all cursor-pointer"
              onClick={() => onMemberSelect(member)}
            >
              <div className="p-6">
                <div className="flex items-center gap-6">
                  <Avatar className="w-16 h-16 border-4 border-white shadow-lg">
                    <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-xl">
                      {member.avatar}
                    </AvatarFallback>
                  </Avatar>

                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="text-2xl font-black text-gray-900">{member.name}</h3>
                      <Badge className={`${
                        member.status === "waiting" ? "bg-yellow-500" :
                        member.status === "in-progress" ? "bg-green-500" :
                        "bg-gray-500"
                      } text-white font-bold border-0`}>
                        {member.status === "waiting" ? "대기중" :
                         member.status === "in-progress" ? "진료중" :
                         "완료"}
                      </Badge>
                    </div>
                    <div className="flex gap-6 text-sm">
                      <div className="flex items-center gap-2 text-gray-700 font-medium">
                        <User className="w-4 h-4" />
                        {member.age}세 / {member.gender}
                      </div>
                      <div className="flex items-center gap-2 text-gray-700 font-medium">
                        <CalendarIcon className="w-4 h-4" />
                        최근 방문: {member.lastVisit}
                      </div>
                      <div className="flex items-center gap-2 text-gray-700 font-medium">
                        <Stethoscope className="w-4 h-4" />
                        {member.condition}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>
      </div>
    </div>
  );
}
