package com.example.demo.repository;

import com.example.demo.domain.Reportable;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ReportRepository<T extends Reportable, R> extends JpaRepository<R, Long> {
    boolean existsByReporterAndTarget(User reporter, T target);
    void deleteByReporterAndTarget(User reporter, T target);
    long countByTarget(T target);
}
