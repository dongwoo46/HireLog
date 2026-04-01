package com.hirelog.api.board.application.port

import com.hirelog.api.board.application.view.BoardLikeStat

interface BoardLikeQuery {
    fun getStat(boardId: Long, memberId: Long): BoardLikeStat
}