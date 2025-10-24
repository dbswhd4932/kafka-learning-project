package com.example.kafka.repository;

import com.example.kafka.entity.ApplicationEventFailureEntity;
import com.example.kafka.enums.ApplicationEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이벤트 실패 이력 Repository
 */
@Repository
public interface ApplicationEventFailureRepository extends JpaRepository<ApplicationEventFailureEntity, Long> {

    /**
     * 재처리되지 않은 실패 이벤트 조회
     */
    List<ApplicationEventFailureEntity> findByRetriedFalse();

    /**
     * 특정 이벤트 타입의 실패 이력 조회
     */
    List<ApplicationEventFailureEntity> findByEventType(ApplicationEventType eventType);

    /**
     * 특정 기간의 실패 이력 조회
     */
    List<ApplicationEventFailureEntity> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}
