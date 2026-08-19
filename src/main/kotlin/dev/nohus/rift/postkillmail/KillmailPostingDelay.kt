package dev.nohus.rift.postkillmail

enum class KillmailPostingDelay(
    val value: Int,
    val displayName: String,
) {
    NoDelay(0, "No delay"),
    OneHour(1, "1 hour"),
    ThreeHours(2, "3 hours"),
    EightHours(3, "8 hours"),
    OneDay(4, "24 hours"),
    ThreeDays(5, "72 hours");

    companion object {
        fun fromValue(value: Int): KillmailPostingDelay {
            return entries.firstOrNull { it.value == value } ?: NoDelay
        }
    }
}
