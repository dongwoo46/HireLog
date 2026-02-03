package com.hirelog.api.common.infra.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version

@MappedSuperclass
abstract class VersionedEntity : BaseEntity() {

    /**
     * Optimistic Lock Version
     *
     * 역할:
     * - 동시 수정 충돌 감지
     * - Lost Update 방지
     */
    @Version
    @Column(nullable = false)
    var version: Long = 0
        protected set
}
