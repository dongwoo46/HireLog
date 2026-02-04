package com.hirelog.api.common.exception

/**
 * Python 파이프라인 실패 정보를 담는 Exception
 */
class PipelineFailedException(
    val errorCode: String,
    errorMessage: String,
    val errorCategory: String,
    val pipelineStage: String,
    val workerHost: String
) : RuntimeException("[$errorCode] $errorMessage (stage=$pipelineStage, category=$errorCategory)")
