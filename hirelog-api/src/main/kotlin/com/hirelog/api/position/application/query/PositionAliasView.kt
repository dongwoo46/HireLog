package com.hirelog.api.position.application.query

import com.hirelog.api.position.domain.PositionAliasStatus

data class PositionAliasView(
    val id: Long,
    val aliasName: String,
    val normalizedAliasName: String,
    val status: PositionAliasStatus,
    val positionId: Long
)
