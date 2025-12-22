import apiClient from "./client";

export interface MedicineItem {
  medicineId?: number;
  medicineName: string;
  dosage: string;
  frequency: string;
  durationDays?: number;
  instructions?: string;
}

export interface PrescriptionRequest {
  appointmentId: number;
  patientId: number;
  diagnosis: string;
  medicines: MedicineItem[];
  additionalInstructions?: string;
}

export interface MedicineResponse {
  id: number;
  code: string;
  name: string;
  manufacturer?: string;
  standardDosage?: string;
  unit?: string;
  description?: string;
  sideEffects?: string;
  precautions?: string;
}

export interface PrescriptionResponse {
  id: number;
  patientId: number;
  patientName: string;
  doctorId: number;
  doctorName: string;
  departmentName?: string;
  appointmentId?: number;
  diagnosis: string;
  date: string;
  medicines: {
    id: number;
    medicineId?: number;
    medicineName: string;
    dosage: string;
    frequency: string;
    durationDays?: number;
    instructions?: string;
  }[];
}

export const prescriptionApi = {
  // 처방전 발급
  create: async (data: PrescriptionRequest): Promise<PrescriptionResponse> => {
    const response = await apiClient.post("/prescriptions", data);
    return response.data;
  },

  // 환자 처방전 목록 조회
  getPatientPrescriptions: async (patientId: number): Promise<PrescriptionResponse[]> => {
    const response = await apiClient.get(`/prescriptions/patient/${patientId}`);
    return response.data;
  },

  // 의사 발급 처방전 목록 조회
  getDoctorPrescriptions: async (doctorId: number): Promise<PrescriptionResponse[]> => {
    const response = await apiClient.get(`/prescriptions/doctor/${doctorId}`);
    return response.data;
  },

  // 처방전 상세 조회
  getById: async (id: number): Promise<PrescriptionResponse> => {
    const response = await apiClient.get(`/prescriptions/${id}`);
    return response.data;
  },

  // 약품 검색
  searchMedicines: async (query: string): Promise<MedicineResponse[]> => {
    const response = await apiClient.get(`/prescriptions/medicines/search`, {
      params: { query },
    });
    return response.data;
  },

  // 전체 약품 목록 조회
  getAllMedicines: async (): Promise<MedicineResponse[]> => {
    const response = await apiClient.get("/prescriptions/medicines");
    return response.data;
  },
};
