package com.hirelog.api.position.application.query

data class ImportResult(
    val total: Int,
    val success: Int,
    val duplicated: Int,
    val failed: Int
)
