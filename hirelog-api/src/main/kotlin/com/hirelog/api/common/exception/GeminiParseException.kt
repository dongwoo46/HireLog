package com.hirelog.api.common.exception

class GeminiParseException(
    cause: Throwable
) : RuntimeException("Failed to parse Gemini response", cause)


