package com.example.demo.repository;

import com.example.demo.domain.entity.DrawPrize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrawPrizeRepository extends JpaRepository<DrawPrize, Long> {

    @Modifying
    @Query("UPDATE DrawPrize p SET p.remainingQuantity = :quantity WHERE p.id = :id")
    void updateRemainingQuantity(@Param("id") Long id, @Param("quantity") int quantity);

    List<DrawPrize> findByRemainingQuantityGreaterThan(int quantity);

    @Query("SELECT p FROM DrawPrize p WHERE p.remainingQuantity > 0 ORDER BY p.probability DESC")
    List<DrawPrize> findAvailablePrizes();
}
