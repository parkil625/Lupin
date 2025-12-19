package com.example.demo.service;
import com.example.demo.domain.entity.Appointment;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.domain.enums.AppointmentStatus;
import com.example.demo.dto.request.AppointmentRequest;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.util.AppointmentTimeUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String APPOINTMENT_LOCK_PREFIX = "appointment:lock:doctor:";
    private static final String BOOKED_TIMES_CACHE_PREFIX = "appointment:booked:";
    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 5L;
    private static final long CACHE_TTL = 300L; // 5분

    @Transactional
    public Long createAppointment(AppointmentRequest request) {
        // 1. 환자 & 의사 존재 여부 확인
        User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "존재하지 않는 환자입니다."));

        User doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "존재하지 않는 의사입니다."));

        if (doctor.getRole() != Role.DOCTOR) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "해당 사용자는 의사가 아닙니다.");
        }

        // Redis 분산 락을 사용하여 동시성 제어
        String lockKey = APPOINTMENT_LOCK_PREFIX + doctor.getId() + ":" + request.getDate();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도 (최대 3초 대기, 5초 후 자동 해제)
            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException(ErrorCode.APPOINTMENT_ALREADY_EXISTS,
                    "다른 사용자가 예약 중입니다. 잠시 후 다시 시도해주세요.");
            }

            // 의사의 해당 시간대 예약 여부 확인 (한 명의 의사는 같은 시간에 한 명만 진료 가능)
            if (appointmentRepository.existsByDoctorIdAndDate(doctor.getId(), request.getDate())) {
                throw new BusinessException(ErrorCode.APPOINTMENT_ALREADY_EXISTS, "해당 의사의 해당 시간에 예약이 이미 꽉 찼습니다.");
            }

            // 환자의 중복 예약 체크는 제거 (다른 진료과는 같은 시간에 예약 가능)

            Appointment appointment = Appointment.builder()
                    .patient(patient)
                    .doctor(doctor)
                    .date(request.getDate())
                    .status(AppointmentStatus.SCHEDULED)
                    .departmentName(doctor.getDepartment())
                    .build();

            Appointment savedAppointment = appointmentRepository.save(appointment);

            // 예약 생성 시 자동으로 채팅방 생성 (메시지 없이)
            String roomId = chatService.createChatRoomForAppointment(savedAppointment.getId());
            log.info("예약 ID {}에 대한 채팅방 생성 완료: {}", savedAppointment.getId(), roomId);

            // 트랜잭션 커밋 후 Redis 캐시 무효화 (트랜잭션이 성공적으로 완료된 후에만 캐시 무효화)
            Long doctorIdFinal = doctor.getId();
            LocalDate dateFinal = request.getDate().toLocalDate();

            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                // 트랜잭션이 활성화된 경우 (실제 운영 환경)
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        invalidateBookedTimesCache(doctorIdFinal, dateFinal);
                        log.info("트랜잭션 커밋 후 캐시 무효화 완료: doctorId={}, date={}", doctorIdFinal, dateFinal);
                    }
                });
            } else {
                // 트랜잭션이 없는 경우 (단위 테스트 환경) 즉시 실행
                invalidateBookedTimesCache(doctorIdFinal, dateFinal);
                log.debug("트랜잭션 없음 - 캐시 즉시 무효화: doctorId={}, date={}", doctorIdFinal, dateFinal);
            }

            return savedAppointment.getId();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.APPOINTMENT_ALREADY_EXISTS,
                "예약 처리 중 오류가 발생했습니다. 다시 시도해주세요.");
        } finally {
            // 락 해제
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public List<Appointment> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorIdOrderByDateDesc(doctorId);
    }

    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByDateDesc(patientId);
    }

    @Transactional
    public void cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND, "존재하지 않는 예약입니다."));

        // 엔티티 내부의 비즈니스 로직 호출 (상태 변경 검증 포함)
        appointment.cancel();

        // 트랜잭션 커밋 후 Redis 캐시 무효화 (예약 취소 시 예약 가능 시간이 변경됨)
        Long doctorId = appointment.getDoctor().getId();
        LocalDate date = appointment.getDate().toLocalDate();
        
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                invalidateBookedTimesCache(doctorId, date);
                log.info("예약 취소 후 캐시 무효화 완료: doctorId={}, date={}", doctorId, date);
            }
        });
        
        } else {
        // 트랜잭션이 없는 테스트 환경 등에서는 즉시 캐시 무효화
        invalidateBookedTimesCache(doctorId, date);
        log.debug("트랜잭션 없음 - 예약 취소 후 캐시 즉시 무효화: doctorId={}, date={}", doctorId, date);
        }
    }

    @Transactional
    public void startConsultation(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND, "존재하지 않는 예약입니다."));

        appointment.startConsultation();
    }

    @Transactional
    public void completeConsultation(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND, "존재하지 않는 예약입니다."));

        appointment.complete();
    }

    public List<String> getBookedTimesByDoctorAndDate(Long doctorId, LocalDate date) {
        // Redis 캐시 키 생성
        String cacheKey = BOOKED_TIMES_CACHE_PREFIX + doctorId + ":" + date;

        // 캐시에서 조회
        List<String> cachedTimes = getCachedBookedTimes(cacheKey);
        if (cachedTimes != null) {
            log.debug("캐시에서 예약 시간 조회: doctorId={}, date={}", doctorId, date);
            return cachedTimes;
        }

        // 캐시 미스 시 DB에서 조회
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndDateBetween(
                doctorId, startOfDay, endOfDay
        );

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        List<String> bookedTimes = appointments.stream()
                .filter(apt -> apt.getStatus() != AppointmentStatus.CANCELLED)
                .map(apt -> apt.getDate().format(timeFormatter))
                .collect(Collectors.toList());

        // Redis 캐시에 저장 (5분 TTL)
        cacheBookedTimes(cacheKey, bookedTimes);
        log.debug("DB에서 예약 시간 조회 및 캐시 저장: doctorId={}, date={}", doctorId, date);

        return bookedTimes;
    }

    /**
     * Redis 캐시에서 예약 시간 목록 조회
     */
    private List<String> getCachedBookedTimes(String cacheKey) {
        try {
            Long size = redisTemplate.opsForList().size(cacheKey);
            if (size == null || size == 0) {
                return null;
            }
            return redisTemplate.opsForList().range(cacheKey, 0, -1);
        } catch (Exception e) {
            log.warn("Redis 캐시 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Redis 캐시에 예약 시간 목록 저장
     */
    private void cacheBookedTimes(String cacheKey, List<String> bookedTimes) {
        try {
            redisTemplate.delete(cacheKey);
            if (!bookedTimes.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(cacheKey, bookedTimes);
            } else {
                // 빈 리스트도 캐싱 (불필요한 DB 조회 방지)
                redisTemplate.opsForList().rightPush(cacheKey, "EMPTY");
            }
            redisTemplate.expire(cacheKey, CACHE_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis 캐시 저장 실패: {}", e.getMessage());
        }
    }

    /**
     * 예약 시간 캐시 무효화
     */
    private void invalidateBookedTimesCache(Long doctorId, LocalDate date) {
        String cacheKey = BOOKED_TIMES_CACHE_PREFIX + doctorId + ":" + date;
        try {
            redisTemplate.delete(cacheKey);
            log.debug("예약 시간 캐시 무효화: doctorId={}, date={}", doctorId, date);
        } catch (Exception e) {
            log.warn("Redis 캐시 무효화 실패: {}", e.getMessage());
        }
    }

    /**
     * 채팅 가능 여부 확인 (예약 시간 5분 전부터 가능)
     */
    public boolean isChatAvailable(Long appointmentId) {
        Appointment appointment = appointmentRepository.findByIdWithPatientAndDoctor(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND, "존재하지 않는 예약입니다."));

        return AppointmentTimeUtils.isChatAvailable(appointment.getStatus());
    }

    /**
     * 채팅 잠금 메시지 조회
     */
    public String getChatLockMessage(Long appointmentId) {
        Appointment appointment = appointmentRepository.findByIdWithPatientAndDoctor(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND, "존재하지 않는 예약입니다."));

        return AppointmentTimeUtils.getChatLockMessage(appointment.getDate(), appointment.getStatus());
    }
}
