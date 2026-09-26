package com.scazzumvivendi.seento.data.ble

internal object TransientMdsReadFailure {
    private val statusCodePattern = Regex("(?<!\\d)(408|502)(?!\\d)")

    fun matches(error: Throwable): Boolean {
        val description = describe(error)
        return statusCodePattern.containsMatchIn(description) ||
            listOf("timeout", "timed out", "temporarily unavailable", "gateway")
                .any(description::contains)
    }

    private fun describe(error: Throwable): String = generateSequence(error) { it.cause }
        .joinToString(" ") { "${it::class.java.simpleName}: ${it.message.orEmpty()}" }
        .lowercase()
}
