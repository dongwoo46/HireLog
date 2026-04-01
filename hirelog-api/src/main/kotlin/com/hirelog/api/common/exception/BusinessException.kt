package com.hirelog.api.common.exception

class BusinessException(
    val errorCode: BusinessErrorCode,
    override val message: String = errorCode.defaultMessage
) : RuntimeException(message)
