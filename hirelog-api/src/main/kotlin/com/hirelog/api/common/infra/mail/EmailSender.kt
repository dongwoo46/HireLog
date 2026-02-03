package com.hirelog.api.common.infra.mail

/**
 * 이메일 발송 포트
 */
interface EmailSender {
    fun send(to: String, subject: String, body: String)
}
