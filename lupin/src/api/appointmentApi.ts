import apiClient from "./client";

/**
 * 예약 요청 DTO
 */
export interface AppointmentRequest {
  patientId: number;
  doctorId: number;
  date: string; // ISO 8601 형식 "2025-12-10T15:00:00"
}

/**
 * 예약 응답 DTO
 */
export interface AppointmentResponse {
  id: number;
  patientId: number;
  patientName: string;
  doctorId: number;
  doctorName: string;
  departmentName?: string;
  date: string;
  status: "SCHEDULED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
}

export const appointmentApi = {
  /**
   * 예약 생성
   * POST /api/appointment
   */
  createAppointment: async (request: AppointmentRequest): Promise<number> => {
    const response = await apiClient.post("/appointment", request);
    return response.data; // 예약 ID 반환
  },

  /**
   * 의사의 예약 목록 조회
   * GET /api/appointment/doctor/{doctorId}
   */
  getDoctorAppointments: async (
    doctorId: number
  ): Promise<AppointmentResponse[]> => {
    const response = await apiClient.get(`/appointment/doctor/${doctorId}`);
    return response.data;
  },

  /**
   * 환자의 예약 목록 조회
   * GET /api/appointment/patient/{patientId}
   */
  getPatientAppointments: async (
    patientId: number
  ): Promise<AppointmentResponse[]> => {
    const response = await apiClient.get(`/appointment/patient/${patientId}`);
    return response.data;
  },

  /**
   * 예약 취소
   * PUT /api/appointment/{appointmentId}/cancel
   */
  cancelAppointment: async (appointmentId: number): Promise<void> => {
    await apiClient.put(`/appointment/${appointmentId}/cancel`);
  },
};
