package com.hirelog.api.common.infra.storage

import com.hirelog.api.common.logging.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * 파일 저장 서비스
 *
 * 책임:
 * - 업로드된 파일을 로컬 파일 시스템에 저장
 * - 저장된 파일 경로 반환
 *
 * 저장 경로 구조:
 * {baseDir}/{yyyy}/{MM}/{dd}/{uuid}_{originalFilename}
 */
@Service
class FileStorageService(
    @Value("\${app.storage.base-dir}")
    private val baseDir: String
) {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        private val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    }

    /**
     * 이미지 파일 저장
     *
     * @param file 업로드된 파일
     * @param subDir 하위 디렉토리 (예: "ocr", "jd")
     * @return 저장된 파일의 절대 경로
     */
    fun saveImage(file: MultipartFile, subDir: String = "ocr"): String {
        validateImageFile(file)

        val today = LocalDate.now()
        val datePath = today.format(DATE_FORMATTER)
        val targetDir = Paths.get(baseDir, subDir, datePath)

        // 디렉토리 생성
        Files.createDirectories(targetDir)

        // 파일명 생성: UUID_원본파일명
        val originalFilename = file.originalFilename ?: "unknown"
        val extension = originalFilename.substringAfterLast(".", "").lowercase()

        val savedFilename = "${UUID.randomUUID()}.$extension"
        val targetPath = targetDir.resolve(savedFilename)
        
        // 파일저장
        file.inputStream.use { input ->
            Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }

        log.info(
            "[FILE_SAVED] originalName={}, savedPath={}, size={}",
            originalFilename,
            targetPath,
            file.size
        )

        return targetPath.toAbsolutePath().toString()
    }

    /**
     * 여러 이미지 파일 저장
     *
     * @return 저장된 파일 경로 목록
     */
    fun saveImages(files: List<MultipartFile>, subDir: String = "ocr"): List<String> {
        return files.map { saveImage(it, subDir) }
    }

    private fun validateImageFile(file: MultipartFile) {
        require(!file.isEmpty) { "파일이 비어있습니다." }

        val originalFilename = file.originalFilename ?: ""
        val extension = originalFilename.substringAfterLast(".", "").lowercase()

        require(extension in ALLOWED_IMAGE_EXTENSIONS) {
            "지원하지 않는 이미지 형식입니다: $extension (지원: ${ALLOWED_IMAGE_EXTENSIONS.joinToString()})"
        }
    }

}
