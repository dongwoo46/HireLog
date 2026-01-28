package com.hirelog.api.common.infra.jpa

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseEntity {

    /**
     * Optimistic Lock Version
     *
     * 역할:
     * - 동시 수정 충돌 감지
     * - Lost Update 방지
     */
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    /**
     * 생성 시각
     *
     * 규칙:
     * - INSERT 시 Hibernate가 반드시 세팅
     * - 엔티티 외부에서는 null 불가
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        protected set

    /**
     * 수정 시각
     *
     * 규칙:
     * - UPDATE 시 Hibernate가 반드시 세팅
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: LocalDateTime
        protected set
}
