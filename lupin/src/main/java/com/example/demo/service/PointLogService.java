package com.example.demo.service;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.PointLogCreateRequest;
import com.example.demo.dto.response.PointLogResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.PointLogRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 포인트 로그 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointLogService {

    private final PointLogRepository pointLogRepository;
    private final UserRepository userRepository;

    /**
     * 포인트 로그 생성
     */
    @Transactional
    public PointLogResponse createPointLog(PointLogCreateRequest request) {
        User user = findUserById(request.getUserId());

        PointLog pointLog = PointLog.builder()
                .amount(request.getAmount())
                .reason(request.getReason())
                .refId(request.getRefId())
                .build();

        pointLog.setUser(user);

        PointLog savedPointLog = pointLogRepository.save(pointLog);

        log.info("포인트 로그 생성 완료 - pointLogId: {}, userId: {}, amount: {}, reason: {}",
                savedPointLog.getId(), request.getUserId(), request.getAmount(), request.getReason());

        return PointLogResponse.from(savedPointLog);
    }

    /**
     * 특정 사용자의 포인트 로그 조회 (페이징)
     */
    public Page<PointLogResponse> getPointLogsByUserId(Long userId, Pageable pageable) {
        return pointLogRepository.findByUserId(userId, pageable)
                .map(PointLogResponse::from);
    }

    /**
     * 특정 사용자의 포인트 로그 조회 (전체)
     */
    public List<PointLogResponse> getAllPointLogsByUserId(Long userId) {
        return pointLogRepository.findByUserId(userId)
                .stream()
                .map(PointLogResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 포인트 로그 상세 조회
     */
    public PointLogResponse getPointLogDetail(Long pointLogId) {
        PointLog pointLog = findPointLogById(pointLogId);
        return PointLogResponse.from(pointLog);
    }

    /**
     * 특정 사용자의 포인트 합계 조회
     */
    public Long getTotalPointsByUserId(Long userId) {
        return pointLogRepository.sumPointsByUserId(userId);
    }

    /**
     * 특정 기간 내 포인트 로그 조회
     */
    public List<PointLogResponse> getPointLogsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return pointLogRepository.findByUserIdAndDateRange(userId, startDate, endDate)
                .stream()
                .map(PointLogResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사유의 포인트 로그 조회
     */
    public Page<PointLogResponse> getPointLogsByReason(Long userId, String reason, Pageable pageable) {
        return pointLogRepository.findByUserIdAndReason(userId, reason, pageable)
                .map(PointLogResponse::from);
    }

    /**
     * 특정 참조 ID의 포인트 로그 조회
     */
    public List<PointLogResponse> getPointLogsByRefId(String refId) {
        return pointLogRepository.findByRefId(refId)
                .stream()
                .map(PointLogResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 기간 내 포인트 합계 조회
     */
    public Long getTotalPointsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        return pointLogRepository.sumPointsByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * 최근 포인트 로그 조회
     */
    public List<PointLogResponse> getRecentPointLogsByUserId(Long userId, int limit) {
        return pointLogRepository.findRecentPointLogsByUserId(userId, PageRequest.of(0, limit))
                .stream()
                .map(PointLogResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 포인트 로그 수 조회
     */
    public Long getPointLogCountByUserId(Long userId) {
        return pointLogRepository.countByUserId(userId);
    }

    /**
     * 포인트 로그 삭제 (관리자용)
     */
    @Transactional
    public void deletePointLog(Long pointLogId) {
        PointLog pointLog = findPointLogById(pointLogId);
        pointLogRepository.delete(pointLog);

        log.info("포인트 로그 삭제 완료 - pointLogId: {}", pointLogId);
    }

    // === 헬퍼 메서드 ===

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private PointLog findPointLogById(Long pointLogId) {
        return pointLogRepository.findById(pointLogId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POINT_LOG_NOT_FOUND));
    }
}
