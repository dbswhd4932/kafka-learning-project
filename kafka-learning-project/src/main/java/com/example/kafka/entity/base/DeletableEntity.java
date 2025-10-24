package com.example.kafka.entity.base;

import com.example.kafka.converter.BooleanToYNConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * 논리 삭제 Base Entity
 * - 물리적 삭제 대신 delete_yn 플래그로 관리
 */
@Getter
@MappedSuperclass
public abstract class DeletableEntity extends AuditingEntity {

    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "delete_yn", columnDefinition = "char(1) DEFAULT 'N'", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    /**
     * 논리 삭제
     */
    public void delete() {
        this.isDeleted = Boolean.TRUE;
    }

    /**
     * 삭제 취소
     */
    public void deleteRollback() {
        this.isDeleted = Boolean.FALSE;
    }
}
