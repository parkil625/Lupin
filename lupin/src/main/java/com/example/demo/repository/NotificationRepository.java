package com.example.demo.repository;

import com.example.demo.domain.entity.Notification;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDescIdDesc(User user);

    boolean existsByUserAndIsReadFalse(User user);

    List<Notification> findByUserAndIsReadFalse(User user);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.refId = :refId AND n.type IN :types")
    void deleteByRefIdAndTypeIn(@Param("refId") String refId, @Param("types") List<String> types);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.refId = :refId AND n.type = :type")
    void deleteByRefIdAndType(@Param("refId") String refId, @Param("type") String type);
}
