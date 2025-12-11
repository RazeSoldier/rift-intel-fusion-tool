package dev.nohus.rift.logs.parse

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class CharacterNameValidator(
    private val settings: Settings,
) {

    fun isValid(name: String): Boolean {
        // 检查是否在忽略列表中
        val ignoredPatterns = settings.ignoredCharacterNamePatterns
        for (pattern in ignoredPatterns) {
            if (name.equals(pattern, ignoreCase = false)) {
                return false
            }
        }
        
        // 检查是否是叠字（重复文本）
        if (isRepeatedText(name)) {
            return false
        }
        
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
    
    private fun isRepeatedText(name: String): Boolean {
        val cleanName = name.filter { it != ' ' }.lowercase()
        if (cleanName.length < 2) return false
        
        // 检测汉字重复（例如：哈哈、谢谢、哈哈哈哈、哈哈哈哈哈）
        val chineseCharPattern = """^([\u4E00-\u9FFF\u3400-\u4DBF])+$""".toRegex()
        if (chineseCharPattern.matches(cleanName)) {
            // 提取单个汉字
            val chars = cleanName.toList()
            if (chars.size >= 2) {
                val firstChar = chars[0]
                // 如果所有字符都相同，或者是两个汉字的重复（AA, AAAA, AAAAAA等），则认为是叠字
                if (chars.all { it == firstChar }) {
                    return true
                }
                // 检测两两重复的模式（ABAB, ABCABC等）
                val matchesRepeatingPattern = checkRepeatingPattern(chars)
                if (matchesRepeatingPattern) {
                    return true
                }
            }
        }
        
        // 检测英文字母/数字重复（例如：hh、ll、11、22等）
        val alphanumericPattern = """^([a-zA-Z0-9])+$""".toRegex()
        if (alphanumericPattern.matches(cleanName)) {
            val chars = cleanName.toList()
            if (chars.size >= 2) {
                val firstChar = chars[0]
                // 如果所有字符都相同
                if (chars.all { it == firstChar }) {
                    return true
                }
                // 检测两两重复的模式
                val matchesRepeatingPattern = checkRepeatingPattern(chars)
                if (matchesRepeatingPattern) {
                    return true
                }
            }
        }
        
        return false
    }
    
    private fun checkRepeatingPattern(chars: List<Char>): Boolean {
        if (chars.size < 4) return false
        
        // 检测周期性重复（例如：ABAB, ABCABC）
        for (period in 1 until chars.size / 2) {
            var isRepeating = true
            val basePattern = chars.take(period)
            
            for (i in period until chars.size) {
                if (chars[i] != basePattern[i % period]) {
                    isRepeating = false
                    break
                }
            }
            
            if (isRepeating) {
                // 确保至少重复了2次
                val fullRepetitions = chars.size / period
                if (fullRepetitions >= 2) {
                    return true
                }
            }
        }
        
        return false
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
            isRepeatedText(name) ->
                "叠字不能作为角色名"
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
