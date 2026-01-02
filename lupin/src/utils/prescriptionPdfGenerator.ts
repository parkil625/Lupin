import html2canvas from "html2canvas";
import jsPDF from "jspdf";
import { PrescriptionResponse } from "@/api/prescriptionApi";

export const generatePrescriptionPDF = async (
  prescription: PrescriptionResponse
) => {
  // 1. Iframe을 생성하여 메인 앱의 스타일(oklch 등)과 격리
  const iframe = document.createElement("iframe");
  iframe.style.position = "fixed";
  iframe.style.width = "0";
  iframe.style.height = "0";
  iframe.style.border = "none";
  iframe.style.zIndex = "-1000";
  document.body.appendChild(iframe);

  try {
    const doc = iframe.contentWindow?.document;
    if (!doc) throw new Error("Iframe document not found");

    // 2. Iframe 내부에 처방전 HTML 작성 (독립적인 스타일 사용)
    doc.open();
    doc.write(`
      <html>
        <head>
          <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;700;900&display=swap" rel="stylesheet">
          <style>
            body {
              margin: 0;
              padding: 20px 30px;
              font-family: 'Noto Sans KR', sans-serif;
              background-color: white;
              width: 210mm;
              min-height: 297mm;
              box-sizing: border-box;
            }
            * { box-sizing: border-box; }
          </style>
        </head>
        <body>
          <div style="border: 2px solid #000; padding: 15px;">
            <div style="text-align: center; border-bottom: 2px solid #000; padding-bottom: 10px; margin-bottom: 15px;">
              <h1 style="font-size: 28px; font-weight: 900; margin: 0; color: #1a1a1a;">처 방 전</h1>
              <span style="font-size: 12px; color: #666;">(Prescription)</span>
            </div>

            <div style="display: flex; justify-content: space-between; margin-bottom: 15px;">
              <div style="flex: 1; margin-right: 10px;">
                <table style="width: 100%; border-collapse: collapse;">
                  <tr>
                    <td style="border: 1px solid #ddd; padding: 8px; background-color: #f8f9fa; font-weight: bold; width: 90px; font-size: 14px;">발급번호</td>
                    <td style="border: 1px solid #ddd; padding: 8px; font-size: 14px;">${
                      prescription.id
                    }</td>
                  </tr>
                  <tr>
                    <td style="border: 1px solid #ddd; padding: 8px; background-color: #f8f9fa; font-weight: bold; font-size: 14px;">발급일자</td>
                    <td style="border: 1px solid #ddd; padding: 8px; font-size: 14px;">${
                      prescription.date
                    }</td>
                  </tr>
                </table>
              </div>
              <div style="flex: 1; margin-left: 10px;">
                <table style="width: 100%; border-collapse: collapse;">
                  <tr>
                    <td style="border: 1px solid #ddd; padding: 8px; background-color: #f8f9fa; font-weight: bold; width: 90px; font-size: 14px;">의료기관</td>
                    <td style="border: 1px solid #ddd; padding: 8px; font-size: 14px;">루핀 병원</td>
                  </tr>
                  <tr>
                    <td style="border: 1px solid #ddd; padding: 8px; background-color: #f8f9fa; font-weight: bold; font-size: 14px;">담당의사</td>
                    <td style="border: 1px solid #ddd; padding: 8px; font-size: 14px;">${
                      prescription.doctorName
                    } (인)</td>
                  </tr>
                </table>
              </div>
            </div>

            <div style="margin-bottom: 12px;">
              <h3 style="font-size: 16px; font-weight: bold; border-bottom: 1px solid #000; padding-bottom: 6px; margin-bottom: 8px; color: #000;">환자 정보</h3>
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

            <div style="margin-bottom: 12px;">
              <h3 style="font-size: 16px; font-weight: bold; border-bottom: 1px solid #000; padding-bottom: 6px; margin-bottom: 8px; color: #000;">처방 의약품</h3>
              <table style="width: 100%; border-collapse: collapse; text-align: left;">
                <thead>
                  <tr style="background-color: #f1f3f5;">
                    <th style="border: 1px solid #ddd; padding: 8px; width: 60%; font-size: 13px;">약품명</th>
                    <th style="border: 1px solid #ddd; padding: 8px; font-size: 13px;">비고 / 주의사항</th>
                  </tr>
                </thead>
                <tbody>
                  ${(() => {
                    const medicines = prescription.medicineDetails || [];
                    const rows = [];

                    // 실제 약품 행 추가
                    for (let i = 0; i < medicines.length && i < 5; i++) {
                      const med = medicines[i];
                      rows.push(`
                    <tr>
                      <td style="border: 1px solid #ddd; padding: 8px; font-weight: 500; font-size: 13px;">${
                        med.name
                      }</td>
                      <td style="border: 1px solid #ddd; padding: 8px; color: #d35400; font-size: 12px; word-wrap: break-word; white-space: normal; overflow-wrap: break-word;">${
                        med.precautions || "-"
                      }</td>
                    </tr>`);
                    }

                    // 나머지 빈 행 추가 (총 5개가 되도록)
                    for (let i = medicines.length; i < 5; i++) {
                      rows.push(`
                    <tr>
                      <td style="border: 1px solid #ddd; padding: 8px; font-size: 13px;">&nbsp;</td>
                      <td style="border: 1px solid #ddd; padding: 8px; font-size: 12px;">&nbsp;</td>
                    </tr>`);
                    }

                    return rows.join("");
                  })()}
                </tbody>
              </table>
            </div>

            <div style="margin-bottom: 15px;">
              <h3 style="font-size: 16px; font-weight: bold; border-bottom: 1px solid #000; padding-bottom: 6px; margin-bottom: 8px; color: #000;">복용 지도 및 참고사항</h3>
              <div style="border: 1px solid #ddd; padding: 15px; min-height: 150px; background-color: #f8f9fa; border-radius: 8px;">
                <p style="margin: 0; font-size: 13px; line-height: 1.5; color: #333; white-space: pre-wrap; word-wrap: break-word;">${
                  prescription.instructions || "특이사항 없음"
                }</p>
              </div>
            </div>

            <div style="text-align: center; margin-top: 30px; color: #888; font-size: 11px; padding-top: 15px;">
              <p style="margin: 3px 0;">본 처방전은 Lupin 헬스케어 플랫폼에서 발급되었습니다.</p>
              <p style="margin: 3px 0;">© 2025 Lupin All Rights Reserved.</p>
            </div>
          </div>
        </body>
      </html>
    `);
    doc.close();

    // 3. 폰트 로딩 대기 (잠시 지연)
    await new Promise((resolve) => setTimeout(resolve, 500));

    // 4. Iframe 내용을 캡처
    const canvas = await html2canvas(doc.body, {
      scale: 2,
      useCORS: true,
      logging: false,
      backgroundColor: "#ffffff", // 배경색 강제 지정
    });

    const imgData = canvas.toDataURL("image/png");
    const pdf = new jsPDF("p", "mm", "a4");
    const imgWidth = 210; // A4 너비 (mm)
    const pageHeight = 297; // A4 높이 (mm)
    const imgHeight = (canvas.height * imgWidth) / canvas.width;

    // 여러 페이지로 분할하여 추가
    let heightLeft = imgHeight;
    let position = 0;

    // 첫 페이지 추가
    pdf.addImage(imgData, "PNG", 0, position, imgWidth, imgHeight);
    heightLeft -= pageHeight;

    // 내용이 한 페이지를 초과하는 경우 추가 페이지 생성
    while (heightLeft > 0) {
      position = heightLeft - imgHeight;
      pdf.addPage();
      pdf.addImage(imgData, "PNG", 0, position, imgWidth, imgHeight);
      heightLeft -= pageHeight;
    }

    pdf.save(`처방전_${prescription.patientName}_${prescription.date}.pdf`);
  } catch (error) {
    console.error("PDF 생성 중 오류 발생:", error);
    alert("처방전 다운로드에 실패했습니다.");
  } finally {
    // 5. Iframe 삭제
    if (document.body.contains(iframe)) {
      document.body.removeChild(iframe);
    }
  }
};
