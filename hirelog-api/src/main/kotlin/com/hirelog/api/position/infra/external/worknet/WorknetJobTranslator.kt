package com.hirelog.api.position.infra.external.worknet

import com.hirelog.api.position.presentation.debug.dto.WorknetJobDebugDto
import org.springframework.stereotype.Component
import javax.xml.parsers.DocumentBuilderFactory

/**
 * WorknetJobTranslator
 *
 * 책임:
 * - Worknet XML 응답 → Debug DTO 변환
 */
@Component
class WorknetJobTranslator {

    fun toDebugDtos(xml: String): List<WorknetJobDebugDto> {

        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(xml.byteInputStream())

        val jobNodes = document.getElementsByTagName("jobList")

        val result = mutableListOf<WorknetJobDebugDto>()

        for (i in 0 until jobNodes.length) {
            val node = jobNodes.item(i)
            val children = node.childNodes

            var jobCode: String? = null
            var jobName: String? = null
            var categoryCode: String? = null
            var categoryName: String? = null

            for (j in 0 until children.length) {
                val child = children.item(j)
                when (child.nodeName) {
                    "jobCd" -> jobCode = child.textContent
                    "jobNm" -> jobName = child.textContent
                    "jobClcd" -> categoryCode = child.textContent
                    "jobClcdNM" -> categoryName = child.textContent
                }
            }

            if (!jobName.isNullOrBlank() && !jobCode.isNullOrBlank()) {
                result += WorknetJobDebugDto(
                    jobCode = jobCode,
                    jobName = jobName.trim(),
                    categoryCode = categoryCode,
                    categoryName = categoryName
                )
            }
        }

        return result
    }
}
