package dev.nohus.rift.logs.parse

import org.koin.core.annotation.Single

@Single
class CharacterNameValidator {

    fun isValid(name: String): Boolean {
        return PLAYER_NAME_REGEX.matches(name) && PLAYER_NAME_REGEX2.matches(name)
    }

    fun getInvalidReason(name: String): String {
        return when {
            !name.matches(ALLOWED_CHARACTERS_REGEX) ->
                "EVE character names may only contain letters (including Chinese), digits, spaces, \"'\", and \"-\""
            name.length !in 3..37 ->
                "EVE character names must be between 3 and 37 characters long"
            !PLAYER_NAME_REGEX2.matches(name) ->
                "EVE character names cannot start or end with a \"'\" or \"-\""
            else -> "Invalid character name"
        }
    }

    companion object {
        private val PLAYER_NAME_REGEX = """^[\p{L}0-9 '-]{3,37}$""".toRegex()
        private val PLAYER_NAME_REGEX2 = """[^ '-].*[^ '-]""".toRegex()
        private val ALLOWED_CHARACTERS_REGEX = """^[\p{L}0-9 '-]+$""".toRegex()


        // PLAYER_NAME_REGEX, separated into two for reason giving
        private val PLAYER_NAME_REASON_CHARACTERS_REGEX = """[A-z0-9 '-]*""".toRegex()
        private val PLAYER_NAME_REASON_LENGTH_REGEX = """.{3,37}""".toRegex()
    }
}
