package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import dev.nohus.rift.di.koin
import dev.nohus.rift.map.systemcolor.EntityColorRepository
import dev.nohus.rift.network.esi.models.CorporationPalette
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.utils.multiplyBrightness
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

data class CorporationColors(
    val main: Color,
    val secondary: Color?,
    val tertiary: Color?,
)

private val defaultColors = CorporationColors(Color.Transparent, null, null)

@Composable
fun produceCorporationColors(corporationId: Int, palette: CorporationPalette?): State<CorporationColors> {
    if (palette.isValid()) {
        val colors = CorporationColors(
            main = palette.mainColor,
            secondary = palette.secondaryColor,
            tertiary = palette.tertiaryColor,
        )
        return mutableStateOf(colors)
    } else {
        val entityColorRepository: EntityColorRepository = remember { koin.get() }
        val initial = remember(corporationId) {
            entityColorRepository.getCorporationColorOrNull(Originator.FreelanceJobs, corporationId)?.getCorporationColors()
        }
        return produceState(initialValue = initial ?: defaultColors, corporationId) {
            if (initial == null) {
                val color = entityColorRepository.getCorporationColor(Originator.FreelanceJobs, corporationId)
                if (color != null) {
                    value = color.getCorporationColors()
                }
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
private fun CorporationPalette?.isValid(): Boolean {
    contract { returns(true) implies (this@isValid != null) }
    if (this == null) return false
    if (mainColor == Color.White && secondaryColor == null && tertiaryColor == null) return false
    return true
}

private fun Color.getCorporationColors() = CorporationColors(
    main = this,
    secondary = null,
    tertiary = null,
)
