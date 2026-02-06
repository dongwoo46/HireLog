package com.hirelog.api.company.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.common.utils.Normalizer
import jakarta.persistence.*

@Entity
@Table(
    name = "company",
    indexes = [
        Index(
            name = "idx_company_normalized_name",
            columnList = "normalized_name",
            unique = true
        )
    ]
)
class Company protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 대표 회사명 (법인명)
     * 예: 비바리퍼블리카
     */
    @Column(name = "name", nullable = false, length = 200)
    val name: String,

    /**
     * 시스템 기준 정규화 회사명
     *
     * - 중복 판단
     * - 검색 키
     */
    @Column(name = "normalized_name", nullable = false, length = 200)
    val normalizedName: String,

    /**
     * 회사 데이터 출처
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    val source: CompanySource,

    /**
     * 외부 시스템 식별자
     *
     * 예:
     * - DART corpCode
     * - 사업자등록번호
     */
    @Column(name = "external_id", length = 100)
    val externalId: String?,

    /**
     * 사용 여부
     *
     * - 삭제 대신 논리적 비활성화
     */
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

) : BaseEntity() {

    companion object {

        /**
         * Company 생성 팩토리
         *
         * 정책:
         * - 관리자에 의한 명시적 생성
         * - normalizedName은 내부 규칙으로 생성
         */
        fun create(
            name: String,
            source: CompanySource,
            externalId: String?
        ): Company {

            return Company(
                name = name,
                normalizedName = Normalizer.normalizeCompany(name),
                source = source,
                externalId = externalId,
                isActive = true
            )
        }

    }

    /**
     * 회사 비활성화
     *
     * - 참조 무결성 유지
     * - 중복 호출 무시
     */
    fun deactivate() {
        if (!isActive) return
        isActive = false
    }

    /**
     * 회사 활성화
     *
     * - 비활성 상태에서만 복구
     */
    fun activate() {
        if (isActive) return
        isActive = true
    }
}
