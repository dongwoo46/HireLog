package com.hirelog.api.common.infra.mail

import com.hirelog.api.common.logging.log
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class SmtpEmailSender(
    private val mailSender: JavaMailSender,
) : EmailSender {

    @Async
    override fun send(to: String, subject: String, body: String) {
        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(body, true)

            mailSender.send(message)
            log.info("[EMAIL_SENT] to={}, subject={}", to, subject)
        } catch (e: Exception) {
            log.error("[EMAIL_SEND_FAILED] to={}, subject={}, error={}", to, subject, e.message)
        }
    }
}
