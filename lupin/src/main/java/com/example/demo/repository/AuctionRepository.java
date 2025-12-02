package com.example.demo.repository;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.domain.enums.AuctionStatus;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.id = :id")
    Optional<Auction> findByIdForUpdate(@Param("id") Long id);

    //경매 시작시간이 된 경매 찾기
    List<Auction> findByStatusAndStartTimeBefore(
            AuctionStatus status,
            LocalDateTime now
    );

    //경매시간이 다끝난 경매 찾기
    @Query("""
    select a
    from Auction a
    where a.status = 'ACTIVE'
      and (
            (a.overtimeStarted = false and a.regularEndTime < :now)
            or
            (a.overtimeStarted = true and a.overtimeEndTime < :now)
          )
""")
    List<Auction> findExpiredAuctions(@Param("now") LocalDateTime now);


    //현재 진행중인 경매 정보, 경매 물품 정보 가지고 오기
    @Query(""" 
    select a 
    from Auction a join fetch a.auctionItem 
    where a.status = :status """) Optional<Auction> findFirstWithItemByStatus(@Param("status") AuctionStatus status);

}
