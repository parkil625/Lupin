package com.example.demo.repository;

import com.example.demo.domain.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    Optional<Medicine> findByCode(String code);

    Optional<Medicine> findByName(String name);

    List<Medicine> findByNameContainingIgnoreCase(String name);

    List<Medicine> findByOrderByNameAsc();

    boolean existsByCode(String code);
}
