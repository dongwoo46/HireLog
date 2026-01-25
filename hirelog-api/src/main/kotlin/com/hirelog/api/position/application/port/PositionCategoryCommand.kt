package com.hirelog.api.position.application.port

import com.hirelog.api.position.domain.PositionCategory

interface PositionCategoryCommand {

    fun save(positionCategory: PositionCategory): PositionCategory
}
