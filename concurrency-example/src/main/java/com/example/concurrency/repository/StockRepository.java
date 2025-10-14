package com.example.concurrency.repository;

import com.example.concurrency.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Stock 엔티티에 대한 Repository
 *
 * Spring Data JPA를 사용하여 기본적인 CRUD 기능을 제공합니다.
 * synchronized 학습을 위해 별도의 락 관련 메서드는 제공하지 않습니다.
 */
public interface StockRepository extends JpaRepository<Stock, Long> {
    // 기본 JpaRepository 메서드만 사용
    // - save()
    // - findById()
    // - saveAndFlush()
    // - deleteAll()
}
