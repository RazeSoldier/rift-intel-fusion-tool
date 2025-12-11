package dev.nohus.rift.logs.parse

import org.koin.core.annotation.Single

@Single
class CharacterNameValidator {

    fun isValid(name: String): Boolean {
        // 拒绝纯数字
        if (name.matches("""^[0-9\s]+$""".toRegex())) {
            return false
        }
        
        // 允许基本的字符检查和长度检查（考虑汉字）
        val hasValidCharacters = ALLOWED_CHARACTERS_REGEX.matches(name)
        val validStartEnd = PLAYER_NAME_REGEX2.matches(name)
        val validLength = isValidLength(name)
        
        return hasValidCharacters && validStartEnd && validLength
    }
    
    private fun isValidLength(name: String): Boolean {
        // 计算有效字符数（不计空格）
        val charCount = name.filter { it != ' ' }.length
        
        // 如果包含汉字，最少需要2个汉字或混合内容
        // 如果全是拉丁字母和数字的组合，需要至少3个字符
        val hasChineseChars = name.any { it.code in 0x4E00..0x9FFF || it.code in 0x3400..0x4DBF }
        
        return when {
            hasChineseChars -> {
                // 包含汉字的名字：至少2个汉字+其他字符，或至少2个字符总数（如两个汉字）
                charCount >= 2 && charCount <= 37
            }
            else -> {
                // 纯拉丁字母和数字：需要至少3个字符，但要排除纯数字
                charCount >= 3 && charCount <= 37
            }
        }
    }

    fun getInvalidReason(name: String): String {
        return when {
            name.matches("""^[0-9\s]+$""".toRegex()) ->
                "纯数字不能作为角色名"
            !name.matches(ALLOWED_CHARACTERS_REGEX) ->
                "EVE character names may only contain letters (including Chinese), digits, spaces, \"'\" , and \"-\""
            !isValidLength(name) -> {
                val hasChineseChars = name.any { it.code in 0x4E00..0x9FFF || it.code in 0x3400..0x4DBF }
                if (hasChineseChars) {
                    "包含汉字的角色名至少需要2个字符"
                } else {
                    "EVE character names must be between 3 and 37 characters long"
                }
            }
            !PLAYER_NAME_REGEX2.matches(name) ->
                "EVE character names cannot start or end with a \"'\" or \"-\""
            else -> "Invalid character name"
        }
    }

    companion object {
        private val ALLOWED_CHARACTERS_REGEX = """^[\p{L}0-9 '-]+$""".toRegex()
        private val PLAYER_NAME_REGEX2 = """[^ '-].*[^ '-]""".toRegex()
    }
}
