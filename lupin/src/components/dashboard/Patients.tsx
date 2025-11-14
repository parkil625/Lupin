import { Card } from "../ui/card";
import { Badge } from "../ui/badge";
import { Avatar, AvatarFallback } from "../ui/avatar";
import { User, Calendar as CalendarIcon, Stethoscope } from "lucide-react";
import { Patient } from "@/types/dashboard.types";

interface PatientsProps {
  patients: Patient[];
  setSelectedPatient: (patient: Patient) => void;
}

export default function Patients({
  patients,
  setSelectedPatient,
}: PatientsProps) {
  return (
    <div className="h-full overflow-auto p-8">
      <div className="max-w-7xl mx-auto space-y-6">
        <div>
          <h1 className="text-5xl font-black text-gray-900 mb-2">환자 목록</h1>
          <p className="text-gray-700 font-medium text-lg">오늘의 진료 환자</p>
        </div>

        <div className="grid gap-4">
          {patients.map((patient) => (
            <Card
              key={patient.id}
              className="backdrop-blur-2xl bg-white/60 border border-gray-200 shadow-xl hover:shadow-2xl transition-all cursor-pointer"
              onClick={() => setSelectedPatient(patient)}
            >
              <div className="p-6">
                <div className="flex items-center gap-6">
                  <Avatar className="w-16 h-16 border-4 border-white shadow-lg">
                    <AvatarFallback className="bg-gradient-to-br from-gray-600 to-gray-800 text-white font-black text-xl">
                      {patient.avatar}
                    </AvatarFallback>
                  </Avatar>

                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="text-2xl font-black text-gray-900">
                        {patient.name}
                      </h3>
                      <Badge
                        className={`${
                          patient.status === "waiting"
                            ? "bg-yellow-500"
                            : patient.status === "in-progress"
                            ? "bg-green-500"
                            : "bg-gray-500"
                        } text-white font-bold border-0`}
                      >
                        {patient.status === "waiting"
                          ? "대기중"
                          : patient.status === "in-progress"
                          ? "진료중"
                          : "완료"}
                      </Badge>
                    </div>
                    <div className="flex gap-6 text-sm">
                      <div className="flex items-center gap-2 text-gray-700 font-medium">
                        <User className="w-4 h-4" />
                        {patient.age}세 / {patient.gender}
                      </div>
                      <div className="flex items-center gap-2 text-gray-700 font-medium">
                        <CalendarIcon className="w-4 h-4" />
                        최근 방문: {patient.lastVisit}
                      </div>
                      <div className="flex items-center gap-2 text-gray-700 font-medium">
                        <Stethoscope className="w-4 h-4" />
                        {patient.condition}
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
