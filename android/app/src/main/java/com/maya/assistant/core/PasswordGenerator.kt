package com.maya.assistant.core

import java.security.SecureRandom

/** Generates cryptographically random passwords locally - never sent to any server. */
object PasswordGenerator {
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#\$%^&*()-_=+[]{}"

    fun generate(length: Int = 16, includeSymbols: Boolean = true): String {
        val pool = buildString {
            append(LOWER)
            append(UPPER)
            append(DIGITS)
            if (includeSymbols) append(SYMBOLS)
        }
        val random = SecureRandom()
        return (1..length).map { pool[random.nextInt(pool.length)] }.joinToString("")
    }
}
