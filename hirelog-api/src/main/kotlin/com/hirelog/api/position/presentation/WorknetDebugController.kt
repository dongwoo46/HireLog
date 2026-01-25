package com.hirelog.api.position.presentation.debug

import com.hirelog.api.common.logging.log
import com.hirelog.api.position.infra.external.worknet.WorknetJobApiClient
import com.hirelog.api.position.infra.external.worknet.WorknetJobTranslator
import com.hirelog.api.position.infra.external.worknet.dto.WorknetJobRawView
import com.hirelog.api.position.presentation.debug.dto.WorknetJobDebugDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * WorknetDebugController
 *
 * ⚠️ 임시 / 개발자 전용 컨트롤러
 *
 * 책임:
 * - Worknet API 응답 구조 확인
 * - 직업명 데이터 탐색
 *
 * 주의:
 * - DB 저장 ❌
 * - 도메인 로직 ❌
 * - 운영 노출 ❌
 */
@RestController
@RequestMapping("/debug/worknet")
class WorknetDebugController(
    private val apiClient: WorknetJobApiClient,
    private val translator: WorknetJobTranslator
) {


    @GetMapping("/jobs")
    fun fetchJobs(): List<WorknetJobDebugDto> {

        val rawXml = apiClient.fetchJobListRaw()
        val jobs = translator.toDebugDtos(rawXml)

        log.info("[WORKNET] total={}", jobs.size)

        return jobs
    }
}


