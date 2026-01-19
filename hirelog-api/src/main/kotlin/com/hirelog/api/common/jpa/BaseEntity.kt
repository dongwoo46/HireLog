package com.hirelog.api.common.jpa

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set
}
