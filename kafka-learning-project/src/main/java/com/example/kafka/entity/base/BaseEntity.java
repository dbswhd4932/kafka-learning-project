package com.example.kafka.entity.base;

import com.example.kafka.enums.UserType;
import com.example.kafka.security.AccessUserManager;
import jakarta.persistence.*;
import lombok.Getter;

/**
 * 모든 Entity의 Base Entity
 * - 감사 정보 + 논리 삭제 + 사용자 유형
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity extends DeletableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "created_user_type", nullable = false, updatable = false, columnDefinition = "varchar(50) COMMENT '등록회원유형'")
    private UserType createdUserType;

    @Enumerated(EnumType.STRING)
    @Column(name = "modified_user_type", columnDefinition = "varchar(50) COMMENT '수정회원유형'")
    private UserType modifiedUserType;

    /**
     * 저장 전 호출
     * - 등록자 유형 설정
     */
    @PrePersist
    public void prePersist() {
        this.modifiedUserType =
        this.createdUserType = AccessUserManager.getAccessUser().getUserType();
    }

    /**
     * 수정 전 호출
     * - 수정자 유형 설정
     */
    @PreUpdate
    public void preUpdate() {
        this.modifiedUserType = AccessUserManager.getAccessUser().getUserType();
    }
}
