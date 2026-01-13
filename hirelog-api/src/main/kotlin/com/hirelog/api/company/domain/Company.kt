package com.hirelog.api.company.domain

import com.hirelog.api.common.jpa.StringListJsonConverter
import jakarta.persistence.*
import java.time.LocalDateTime

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
class Company(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 대표 회사명 (보통 법인명)
     * 예: 비바리퍼블리카
     */
    @Column(name = "name", nullable = false, length = 200)
    val name: String,

    /**
     * 정규화된 회사명
     * (중복 방지 / 매핑 기준)
     */
    @Column(name = "normalized_name", nullable = false, length = 200)
    val normalizedName: String,

    /**
     * 회사 별칭 목록
     * - 법인명 변형
     * - 영문명
     * - 약칭
     *
     * 예: ["(주)비바리퍼블리카", "Viva Republica"]
     *
     * Brand(토스 등)는 여기에 넣지 않는다.
     */
    @Column(name = "aliases", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = StringListJsonConverter::class)
    val aliases: List<String> = emptyList(),

    /**
     * 회사 데이터 출처
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    val source: CompanySource,

    /**
     * 외부 식별자 (선택)
     * 예: 사업자등록번호, 공공데이터 ID
     */
    @Column(name = "external_id", length = 100)
    val externalId: String? = null,

    /**
     * 사용 여부
     * (잘못 매핑된 회사 비활성화용)
     */
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    /**
     * 회사 최초 등록 시각
     */
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
