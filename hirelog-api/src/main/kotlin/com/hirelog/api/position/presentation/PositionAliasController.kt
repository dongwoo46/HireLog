package com.hirelog.api.position.presentation

import com.hirelog.api.position.application.importer.WorknetPositionAliasImportService
import com.hirelog.api.position.application.port.PositionAliasQuery
import com.hirelog.api.position.application.query.ImportResult
import org.springframework.web.bind.annotation.*

/**
 * PositionAliasController
 *
 * 책임:
 * - PositionAlias 관리 API (v1)
 *
 * 버전 정책:
 * - URI 기반 버저닝 (/api/v1)
 */
@RestController
@RequestMapping("/v1/positions")
class PositionAliasController(
    private val importService: WorknetPositionAliasImportService,
    private val aliasQuery: PositionAliasQuery
) {

//    /**
//     * Worknet → PositionAlias Import
//     *
//     * 관리자 전용
//     */
//    @PostMapping("/{positionId}/aliases/import/worknet")
//    fun importFromWorknet(
//        @PathVariable positionId: Long
//    ): ImportResult {
//        return importService.importAliases(positionId)
//    }
//
//    /**
//     * 특정 Position의 Alias 목록 조회
//     */
//    @GetMapping("/{positionId}/aliases")
//    fun listByPosition(
//        @PathVariable positionId: Long
//    ) = aliasQuery.listByPosition(positionId)
}
