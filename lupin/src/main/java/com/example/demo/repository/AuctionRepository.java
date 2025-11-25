package com.example.demo.repository;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.enums.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 경매 리포지토리
 * - 비관적 락(Pessimistic Lock)을 사용한 동시성 제어
 * - 최고 성능/정합성을 위한 쿼리 최적화
 */
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    /**
     * 상태별 경매 조회
     */
    List<Auction> findByStatusOrderByStartTimeDesc(AuctionStatus status);

    /**
     * 활성 경매 조회 (진행 중)
     */
    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' ORDER BY a.startTime DESC")
    List<Auction> findActiveAuctions(@Param("now") LocalDateTime now);

    /**
     * 예정된 경매 조회 (날짜 오름차순)
     */
    @Query("SELECT a FROM Auction a WHERE a.status = 'SCHEDULED' ORDER BY a.startTime ASC")
    List<Auction> findScheduledAuctions(@Param("now") LocalDateTime now);

    /**
     * 종료된 경매 조회
     * - 정규 시간 종료 + 초읽기 미시작
     * - 초읽기 시간 종료
     */
    @Query("SELECT a FROM Auction a WHERE a.status = 'ACTIVE' AND " +
           "((a.regularEndTime <= :now AND a.overtimeStarted = false) OR " +
           "(a.overtimeStarted = true AND a.overtimeEndTime IS NOT NULL AND a.overtimeEndTime <= :now))")
    List<Auction> findExpiredAuctions(@Param("now") LocalDateTime now);

    /**
     * 비관적 락을 사용한 경매 조회 (입찰 시 사용)
     * SELECT FOR UPDATE로 동시 입찰 제어
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithLock(@Param("id") Long id);

    /**
     * 특정 기간 내 경매 조회
     */
    @Query("SELECT a FROM Auction a WHERE a.startTime BETWEEN :start AND :end ORDER BY a.startTime ASC")
    List<Auction> findAuctionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
