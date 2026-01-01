import html2canvas from "html2canvas";
import jsPDF from "jspdf";
import { PrescriptionResponse } from "@/api/prescriptionApi";

export const generatePrescriptionPDF = async (
  prescription: PrescriptionResponse
) => {
  // 1. PDF로 변환할 숨겨진 HTML 요소 생성 (A4 크기 스타일)
  const element = document.createElement("div");
  element.style.position = "fixed";
  element.style.top = "-9999px";
  element.style.left = "-9999px";
  element.style.width = "210mm"; // A4 너비
  element.style.minHeight = "297mm"; // A4 높이
  element.style.backgroundColor = "white";
  element.style.padding = "40px";
  element.style.fontFamily = "'Noto Sans KR', sans-serif"; // 한글 폰트 적용

  // 처방전 HTML 내용 작성
  element.innerHTML = `
    <div style="border: 2px solid #000; padding: 20px; height: 100%;">
      <div style="text-align: center; border-bottom: 2px solid #000; padding-bottom: 20px; margin-bottom: 30px;">
        <h1 style="font-size: 32px; font-weight: 900; margin: 0; color: #1a1a1a;">처 방 전</h1>
        <span style="font-size: 14px; color: #666;">(Prescription)</span>
      </div>

      <div style="display: flex; justify-content: space-between; margin-bottom: 30px;">
        <div style="flex: 1;">
          <table style="width: 100%; border-collapse: collapse; margin-right: 20px;">
            <tr>
              <td style="border: 1px solid #ddd; padding: 8px; background-color: #f8f9fa; font-weight: bold; width: 100px;">발급번호</td>
              <td style="border: 1px solid #ddd; padding: 8px;">${
                prescription.id
              }</td>
            </tr>
            <tr>
              <td style="border: 1px solid #ddd; padding: 8px; background-color: #f8f9fa; font-weight: bold;">발급일자</td>
              <td style="border: 1px solid #ddd; padding: 8px;">${
                prescription.date
              }</td>
            </tr>
          </table>
        </div>
        <div style="flex: 1; margin-left: 20px;">
          <table style="width: 100%; border-collapse: collapse;">
            <tr>
              <td style="border: 1px solid #ddd; padding: 8px; background-color: #f8f9fa; font-weight: bold; width: 100px;">의료기관</td>
              <td style="border: 1px solid #ddd; padding: 8px;">루핀 병원 (Lupin)</td>
            </tr>
            <tr>
              <td style="border: 1px solid #ddd; padding: 8px; background-color: #f8f9fa; font-weight: bold;">담당의사</td>
              <td style="border: 1px solid #ddd; padding: 8px;">${
                prescription.doctorName
              } (인)</td>
            </tr>
          </table>
        </div>
      </div>

      <div style="margin-bottom: 30px;">
        <h3 style="font-size: 18px; font-weight: bold; border-bottom: 1px solid #000; padding-bottom: 10px; margin-bottom: 15px;">환자 정보</h3>
        <table style="width: 100%; border-collapse: collapse;">
          <tr>
            <td style="border: 1px solid #ddd; padding: 10px; background-color: #f8f9fa; font-weight: bold; width: 120px;">성명</td>
            <td style="border: 1px solid #ddd; padding: 10px;">${
              prescription.patientName
            }</td>
            <td style="border: 1px solid #ddd; padding: 10px; background-color: #f8f9fa; font-weight: bold; width: 120px;">진단명</td>
            <td style="border: 1px solid #ddd; padding: 10px;">${
              prescription.diagnosis
            }</td>
          </tr>
        </table>
      </div>

      <div style="margin-bottom: 30px;">
        <h3 style="font-size: 18px; font-weight: bold; border-bottom: 1px solid #000; padding-bottom: 10px; margin-bottom: 15px;">처방 의약품의 명칭, 용법 및 용량</h3>
        <table style="width: 100%; border-collapse: collapse; text-align: left;">
          <thead>
            <tr style="background-color: #f1f3f5;">
              <th style="border: 1px solid #ddd; padding: 10px; width: 60%;">약품명</th>
              <th style="border: 1px solid #ddd; padding: 10px;">비고 / 주의사항</th>
            </tr>
          </thead>
          <tbody>
            ${prescription.medicineDetails
              .map(
                (med) => `
              <tr>
                <td style="border: 1px solid #ddd; padding: 12px; font-weight: 500;">${
                  med.name
                }</td>
                <td style="border: 1px solid #ddd; padding: 12px; color: #e67e22;">${
                  med.precautions || "-"
                }</td>
              </tr>
            `
              )
              .join("")}
          </tbody>
        </table>
      </div>

      <div style="margin-bottom: 40px;">
        <h3 style="font-size: 18px; font-weight: bold; border-bottom: 1px solid #000; padding-bottom: 10px; margin-bottom: 15px;">조제 시 참고사항 및 복용 지도</h3>
        <div style="border: 1px solid #ddd; padding: 20px; min-height: 100px; background-color: #f8f9fa; border-radius: 8px;">
          <p style="margin: 0; font-size: 15px; line-height: 1.6;">${
            prescription.instructions || "특이사항 없음"
          }</p>
        </div>
      </div>

      <div style="text-align: center; margin-top: 50px; color: #888; font-size: 12px;">
        <p>본 처방전은 Lupin 헬스케어 플랫폼에서 발급되었습니다.</p>
        <p>© 2025 Lupin All Rights Reserved.</p>
      </div>
    </div>
  `;

  document.body.appendChild(element);

  try {
    // 2. html2canvas로 이미지를 생성
    const canvas = await html2canvas(element, {
      scale: 2, // 해상도 2배 (선명하게)
      useCORS: true, // 이미지 로딩 문제 방지
      logging: false,
    });

    const imgData = canvas.toDataURL("image/png");

    // 3. jsPDF로 PDF 생성 및 이미지 삽입
    const pdf = new jsPDF("p", "mm", "a4");
    const imgWidth = 210;
    const imgHeight = (canvas.height * imgWidth) / canvas.width;

    pdf.addImage(imgData, "PNG", 0, 0, imgWidth, imgHeight);
    pdf.save(`처방전_${prescription.patientName}_${prescription.date}.pdf`);
  } catch (error) {
    console.error("PDF 생성 중 오류 발생:", error);
    alert("처방전 다운로드에 실패했습니다.");
  } finally {
    // 4. 임시 요소 삭제
    document.body.removeChild(element);
  }
};
