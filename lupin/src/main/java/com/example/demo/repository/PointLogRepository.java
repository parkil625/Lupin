package com.example.demo.repository;

import com.example.demo.domain.entity.PointLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointLogRepository extends JpaRepository<PointLog, Long> {

}
