package com.hirelog.api.board.infrastructure.adapter

import com.hirelog.api.board.application.port.BoardCommand
import com.hirelog.api.board.domain.Board
import com.hirelog.api.board.infrastructure.BoardJpaRepository
import org.springframework.stereotype.Component

@Component
class BoardJpaCommandAdapter(
    private val repository: BoardJpaRepository
) : BoardCommand {

    override fun save(board: Board): Board = repository.save(board)

    override fun findById(id: Long): Board? = repository.findById(id).orElse(null)
}