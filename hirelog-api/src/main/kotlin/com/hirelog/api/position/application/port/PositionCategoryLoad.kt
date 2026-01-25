package com.hirelog.api.position.application.port

import com.hirelog.api.position.domain.PositionCategory

interface PositionCategoryLoad {

    fun getById(id: Long): PositionCategory
}
