package com.hirelog.api.board.infrastructure

import com.hirelog.api.board.domain.Board
import org.springframework.data.jpa.repository.JpaRepository

interface BoardJpaRepository : JpaRepository<Board, Long>