package com.hirelog.api.board.application.port

import com.hirelog.api.board.domain.Board

interface BoardCommand {
    fun save(board: Board): Board
    fun findById(id: Long): Board?
}