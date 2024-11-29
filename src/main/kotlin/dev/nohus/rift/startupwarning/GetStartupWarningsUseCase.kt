package dev.nohus.rift.startupwarning

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class GetStartupWarningsUseCase(
    private val hasNonEnglishEveClient: HasNonEnglishEveClientUseCase,
    private val isRunningMsiAfterburner: IsRunningMsiAfterburnerUseCase,
    private val hasChatLogsDisabled: HasChatLogsDisabledUseCase,
    private val settings: Settings,
) {

    data class StartupWarning(
        val id: String,
        val title: String,
        val description: String,
    )

    suspend operator fun invoke(): List<StartupWarning> {
        return buildList {
            if (hasNonEnglishEveClient()) {
                add(
                    StartupWarning(
                        id = "non-english client",
                        title = "Non-English EVE Client",
                        description = """
                            Your EVE client is set to a language other than English.
                            RIFT features based on reading game logs won't work.
                        """.trimIndent(),
                    ),
                )
            }
            if (isRunningMsiAfterburner()) {
                add(
                    StartupWarning(
                        id = "msi afterburner",
                        title = "MSI Afterburner",
                        description = """
                            You are running MSI Afterburner or RivaTuner, which is known to inject code into RIFT that causes freezes and crashes.
                        """.trimIndent(),
                    ),
                )
            }
            if (hasChatLogsDisabled()) {
                add(
                    StartupWarning(
                        id = "chat logs disabled",
                        title = "Chat logs are disabled",
                        description = """
                            You need to enable the "Log Chat to File" option in EVE Settings, in the Gameplay section. RIFT won't be able to read intel messages or trigger alerts otherwise.
                            
                            This setting is per-account and you have it disabled on at least one account.
                        """.trimIndent(),
                    ),
                )
            }
        }.filter { it.id !in settings.dismissedWarnings }
    }
}
