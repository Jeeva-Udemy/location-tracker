package com.jeeva.locationtracker.util

import kotlin.random.Random

object FamilyCodeGenerator {
    // Excludes visually ambiguous characters (0/O, 1/I) so it's easy to read aloud or copy.
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(length: Int = 6): String =
        (1..length).map { ALPHABET[Random.nextInt(ALPHABET.length)] }.joinToString("")

    fun normalize(rawCode: String): String =
        rawCode.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
}
