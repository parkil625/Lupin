import { useState, useEffect } from "react"; // [중요] useState, useEffect 불러오기
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { User, Calendar as CalendarIcon, Stethoscope } from "lucide-react";
import { Member } from "@/types/dashboard.types";
import { userApi } from "@/api/userApi"; // [중요] API 불러오기

interface MembersPageProps {
  onMemberSelect: (member: Member) => void;
}

export default function MembersPage({ onMemberSelect }: MembersPageProps) {
  // 1. 상태(변수) 만들기
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(true);

  // 2. 화면 켜질 때 데이터 불러오기
  useEffect(() => {
    const fetchMembers = async () => {
      try {
        const userId = Number(localStorage.getItem("userId") || "0");
        // API 호출하여 진짜 데이터 가져오기
        const data = await userApi.getDoctorPatients(userId);
        setMembers(data);
      } catch (error) {
        console.error("환자 목록 로딩 실패:", error);
        setMembers([]);
      } finally {
        setLoading(false);
      }
    };

    fetchMembers();
  }, []);

  // 3. 로딩 중일 때 표시
  if (loading) {
    return (
      <div className="p-8 text-center text-lg font-medium">
        환자 목록을 불러오는 중...
      </div>
    );
  }

  return (
    <div className="h-full overflow-auto p-8">
      <div className="max-w-7xl mx-auto space-y-6">
        <div>
          <h1 className="text-5xl font-black text-gray-900 mb-2">회원 목록</h1>
          <p className="text-gray-700 font-medium text-lg">오늘의 진료 회원</p>
        </div>

        <div className="grid gap-4">
          {members.length === 0 ? (
            <div className="text-center py-20 text-gray-500 bg-white/50 rounded-2xl border border-dashed border-gray-300">
              담당 회원이 없습니다.
            </div>
          ) : (
            members.map((member) => (
              <Card
                key={member.id}
                className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl hover:shadow-2xl transition-all cursor-pointer"
                onClick={() => onMemberSelect(member)}
              >
                <div className="p-6">
                  <div className="flex items-center gap-6">
                    <Avatar className="w-16 h-16 border-4 border-white shadow-lg">
                      <AvatarFallback className="bg-white">
                        <User className="w-8 h-8 text-gray-400" />
                      </AvatarFallback>
                    </Avatar>

                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-2xl font-black text-gray-900">
                          {member.name}
                        </h3>
                        <Badge
                          className={`${
                            member.status === "waiting"
                              ? "bg-yellow-500"
                              : member.status === "in-progress"
                              ? "bg-green-500"
                              : "bg-gray-500"
                          } text-white font-bold border-0`}
                        >
                          {member.status === "waiting"
                            ? "대기중"
                            : member.status === "in-progress"
                            ? "진료중"
                            : "완료"}
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
            ))
          )}
        </div>
      </div>
    </div>
  );
}
