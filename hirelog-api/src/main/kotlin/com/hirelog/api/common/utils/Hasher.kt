package com.hirelog.api.common.utils

import java.security.MessageDigest

object Hasher {

    fun hash(rawText: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(rawText.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
