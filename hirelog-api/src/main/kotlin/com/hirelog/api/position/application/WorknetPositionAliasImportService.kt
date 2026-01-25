package com.hirelog.api.position.application.importer

import com.hirelog.api.position.application.PositionAliasWriteService
import com.hirelog.api.position.infra.external.worknet.WorknetJobApiClient
import org.springframework.stereotype.Service

/**
 * WorknetPositionAliasImportService
 *
 * 책임:
 * - Worknet API 호출
 * - Alias 후보 추출
 * - Alias 생성 유스케이스 호출
 *
 * 정책:
 * - 개별 Alias 실패는 전체 실패로 만들지 않는다
 */
@Service
class WorknetPositionAliasImportService(
    private val apiClient: WorknetJobApiClient,
    private val aliasWriteService: PositionAliasWriteService
) {

    companion object {
        private const val CHUNK_SIZE = 100
    }


}

