package com.example.demo.repository;

import com.example.demo.domain.entity.Prescription;
import com.example.demo.domain.entity.PrescriptionMed;
import com.example.demo.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrescriptionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Test
    @DisplayName("처방전 생성 시 필수 필드가 저장되는지 테스트")
    void shouldSavePrescriptionWithRequiredFields() {
        // given
        User doctor = createAndSaveUser("doctor1", "Dr. Kim");
        User patient = createAndSaveUser("patient1", "Patient Lee");
        LocalDate prescribedDate = LocalDate.of(2025, 12, 1);

        Prescription prescription = Prescription.builder()
                .doctor(doctor)
                .patient(patient)
                .date(prescribedDate)
                .diagnosis("감기")
                .build();

        // when
        Prescription saved = prescriptionRepository.save(prescription);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDoctor()).isEqualTo(doctor);
        assertThat(saved.getPatient()).isEqualTo(patient);
        assertThat(saved.getDate()).isEqualTo(prescribedDate);
        assertThat(saved.getDiagnosis()).isEqualTo("감기");
    }

    @Test
    @DisplayName("처방전 생성 시 약물 정보도 함께 저장되는지 테스트")
    void shouldSavePrescriptionWithMedicines() {
        // given
        User doctor = createAndSaveUser("doctor2", "Dr. Park");
        User patient = createAndSaveUser("patient2", "Patient Kim");
        LocalDate prescribedDate = LocalDate.of(2025, 12, 1);

        Prescription prescription = Prescription.builder()
                .doctor(doctor)
                .patient(patient)
                .date(prescribedDate)
                .diagnosis("고혈압")
                .build();

        PrescriptionMed med1 = PrescriptionMed.builder()
                .medicineName("아스피린")
                .dosage("100mg")
                .frequency("하루 1회")
                .build();

        PrescriptionMed med2 = PrescriptionMed.builder()
                .medicineName("혈압약")
                .dosage("50mg")
                .frequency("하루 2회")
                .build();

        prescription.addMedicine(med1);
        prescription.addMedicine(med2);

        // when
        Prescription saved = prescriptionRepository.save(prescription);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMedicines()).hasSize(2);
        assertThat(saved.getMedicines().get(0).getMedicineName()).isEqualTo("아스피린");
        assertThat(saved.getMedicines().get(0).getDosage()).isEqualTo("100mg");
        assertThat(saved.getMedicines().get(0).getFrequency()).isEqualTo("하루 1회");
        assertThat(saved.getMedicines().get(1).getMedicineName()).isEqualTo("혈압약");
    }

    @Test
    @DisplayName("의사 ID 없이 처방전 생성 시 예외 발생 테스트")
    void shouldThrowExceptionWhenDoctorIsNull() {
        // given
        User patient = createAndSaveUser("patient3", "Patient Choi");
        LocalDate prescribedDate = LocalDate.of(2025, 12, 1);

        Prescription prescription = Prescription.builder()
                .doctor(null)  // 의사 없이 생성
                .patient(patient)
                .date(prescribedDate)
                .diagnosis("감기")
                .build();

        // when & then
        assertThatThrownBy(() -> prescriptionRepository.save(prescription))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("환자 ID 없이 처방전 생성 시 예외 발생 테스트")
    void shouldThrowExceptionWhenPatientIsNull() {
        // given
        User doctor = createAndSaveUser("doctor3", "Dr. Jung");
        LocalDate prescribedDate = LocalDate.of(2025, 12, 1);

        Prescription prescription = Prescription.builder()
                .doctor(doctor)
                .patient(null)  // 환자 없이 생성
                .date(prescribedDate)
                .diagnosis("감기")
                .build();

        // when & then
        assertThatThrownBy(() -> prescriptionRepository.save(prescription))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("환자 ID로 처방전 목록 조회 (최신순)")
    void shouldFindByPatientIdOrderByDateDesc() {
        // given
        User doctor = createAndSaveUser("doctor4", "Dr. Lee");
        User patient = createAndSaveUser("patient4", "Patient Han");

        Prescription prescription1 = Prescription.builder()
                .doctor(doctor)
                .patient(patient)
                .date(LocalDate.of(2025, 11, 1))
                .diagnosis("감기")
                .build();

        Prescription prescription2 = Prescription.builder()
                .doctor(doctor)
                .patient(patient)
                .date(LocalDate.of(2025, 12, 1))
                .diagnosis("고혈압")
                .build();

        Prescription prescription3 = Prescription.builder()
                .doctor(doctor)
                .patient(patient)
                .date(LocalDate.of(2025, 10, 1))
                .diagnosis("두통")
                .build();

        prescriptionRepository.save(prescription1);
        prescriptionRepository.save(prescription2);
        prescriptionRepository.save(prescription3);

        // when
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByDateDesc(patient.getId());

        // then
        assertThat(prescriptions).hasSize(3);
        assertThat(prescriptions.get(0).getDate()).isEqualTo(LocalDate.of(2025, 12, 1));
        assertThat(prescriptions.get(1).getDate()).isEqualTo(LocalDate.of(2025, 11, 1));
        assertThat(prescriptions.get(2).getDate()).isEqualTo(LocalDate.of(2025, 10, 1));
    }

    @Test
    @DisplayName("의사 ID로 처방전 목록 조회 (최신순)")
    void shouldFindByDoctorIdOrderByDateDesc() {
        // given
        User doctor = createAndSaveUser("doctor5", "Dr. Choi");
        User patient1 = createAndSaveUser("patient5", "Patient Song");
        User patient2 = createAndSaveUser("patient6", "Patient Kang");

        Prescription prescription1 = Prescription.builder()
                .doctor(doctor)
                .patient(patient1)
                .date(LocalDate.of(2025, 11, 15))
                .diagnosis("독감")
                .build();

        Prescription prescription2 = Prescription.builder()
                .doctor(doctor)
                .patient(patient2)
                .date(LocalDate.of(2025, 12, 5))
                .diagnosis("알레르기")
                .build();

        Prescription prescription3 = Prescription.builder()
                .doctor(doctor)
                .patient(patient1)
                .date(LocalDate.of(2025, 10, 20))
                .diagnosis("위염")
                .build();

        prescriptionRepository.save(prescription1);
        prescriptionRepository.save(prescription2);
        prescriptionRepository.save(prescription3);

        // when
        List<Prescription> prescriptions = prescriptionRepository.findByDoctorIdOrderByDateDesc(doctor.getId());

        // then
        assertThat(prescriptions).hasSize(3);
        assertThat(prescriptions.get(0).getDate()).isEqualTo(LocalDate.of(2025, 12, 5));
        assertThat(prescriptions.get(1).getDate()).isEqualTo(LocalDate.of(2025, 11, 15));
        assertThat(prescriptions.get(2).getDate()).isEqualTo(LocalDate.of(2025, 10, 20));
    }

    @Test
    @DisplayName("처방전 ID로 상세 조회 (약물 정보 포함)")
    void shouldFindByIdWithMedicines() {
        // given
        User doctor = createAndSaveUser("doctor6", "Dr. Yoon");
        User patient = createAndSaveUser("patient7", "Patient Shin");

        Prescription prescription = Prescription.builder()
                .doctor(doctor)
                .patient(patient)
                .date(LocalDate.of(2025, 12, 1))
                .diagnosis("당뇨")
                .build();

        PrescriptionMed med1 = PrescriptionMed.builder()
                .medicineName("메트포르민")
                .dosage("500mg")
                .frequency("하루 2회")
                .build();

        PrescriptionMed med2 = PrescriptionMed.builder()
                .medicineName("인슐린")
                .dosage("10unit")
                .frequency("하루 1회")
                .build();

        prescription.addMedicine(med1);
        prescription.addMedicine(med2);

        Prescription saved = prescriptionRepository.save(prescription);
        Long prescriptionId = saved.getId();

        // when
        Prescription found = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new AssertionError("처방전을 찾을 수 없습니다"));

        // then
        assertThat(found.getId()).isEqualTo(prescriptionId);
        assertThat(found.getDoctor()).isEqualTo(doctor);
        assertThat(found.getPatient()).isEqualTo(patient);
        assertThat(found.getDiagnosis()).isEqualTo("당뇨");
        assertThat(found.getMedicines()).hasSize(2);
        assertThat(found.getMedicines().get(0).getMedicineName()).isEqualTo("메트포르민");
        assertThat(found.getMedicines().get(1).getMedicineName()).isEqualTo("인슐린");
    }
}
