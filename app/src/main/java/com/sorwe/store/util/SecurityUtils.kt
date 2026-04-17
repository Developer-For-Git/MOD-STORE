package com.sorwe.store.util

object SecurityUtils {

    /**
     * Validates that a URL uses HTTPS protocol.
     */
    fun isHttpsUrl(url: String): Boolean {
        return url.trim().lowercase().startsWith("https://")
    }

    /**
     * Validates basic JSON URL format.
     */
    fun isValidJsonUrl(url: String): Boolean {
        if (!isHttpsUrl(url)) return false
        val trimmed = url.trim()
        return trimmed.length > 10 && !trimmed.contains(" ")
    }

    /**
     * Sanitizes a URL string by trimming whitespace and removing trailing slashes.
     */
    fun sanitizeUrl(url: String): String {
        return url.trim().trimEnd('/')
    }
}
