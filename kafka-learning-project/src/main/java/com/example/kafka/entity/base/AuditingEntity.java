package com.example.kafka.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 감사(Auditing) 정보 Base Entity
 * - 등록자/등록일시/수정자/수정일시 자동 관리
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditingEntity implements Serializable {

    @CreatedBy
    @Column(name = "created_user_id", nullable = false, columnDefinition = "bigint COMMENT '등록자 ID'", updatable = false)
    protected Long createdUserId;

    @CreatedDate
    @Column(name = "created_datetime", nullable = false, columnDefinition = "datetime COMMENT '등록일시'", updatable = false)
    protected LocalDateTime createdDatetime;

    @LastModifiedBy
    @Column(name = "modified_user_id", columnDefinition = "bigint COMMENT '수정자 ID'")
    protected Long modifiedUserId;

    @LastModifiedDate
    @Column(name = "modified_datetime", columnDefinition = "datetime COMMENT '수정일시'")
    protected LocalDateTime modifiedDatetime;
}
