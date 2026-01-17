package com.hirelog.api.common.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

class DuplicateJobSnapshotException(
    message: String = "이미 저장된 Job Description입니다."
) : RuntimeException(message)
