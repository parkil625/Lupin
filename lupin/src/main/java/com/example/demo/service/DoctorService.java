package com.example.demo.service;

import com.example.demo.domain.entity.DoctorProfile;
import com.example.demo.dto.response.DoctorResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.DoctorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorProfileRepository doctorProfileRepository;

    /**
     * 모든 의사 목록 조회
     */
    public List<DoctorResponse> getAllDoctors() {
        List<DoctorProfile> doctorProfiles = doctorProfileRepository.findAllWithUser();
        return doctorProfiles.stream()
                .map(DoctorResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 진료과별 의사 목록 조회
     */
    public List<DoctorResponse> getDoctorsBySpecialty(String specialty) {
        List<DoctorProfile> doctorProfiles = doctorProfileRepository.findBySpecialtyWithUser(specialty);
        return doctorProfiles.stream()
                .map(DoctorResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 의사 ID로 상세 정보 조회
     */
    public DoctorResponse getDoctorById(Long userId) {
        DoctorProfile doctorProfile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "존재하지 않는 의사입니다."));
        return DoctorResponse.from(doctorProfile);
    }
}
