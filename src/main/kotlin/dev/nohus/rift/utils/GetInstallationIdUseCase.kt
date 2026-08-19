package dev.nohus.rift.utils

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single
import java.util.UUID

@Single
class GetInstallationIdUseCase(
    private val settings: Settings,
) {

    operator fun invoke(): String {
        return settings.installationId ?: UUID.randomUUID().toString().also { settings.installationId = it }
    }
}
