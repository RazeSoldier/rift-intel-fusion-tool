package dev.nohus.rift.assets

import dev.nohus.rift.repositories.TypesRepository.Type
import org.koin.core.annotation.Single

@Single
class IsDockableTypeUseCase {

    companion object {
        private const val TYPE_ID_ANSIBLEX_JUMP_BRIDGE = 35841
        private const val TYPE_ID_PHAROLUX_CYNO_BEACON = 35840
        private const val TYPE_ID_TENEBREX_CYNO_JAMMER = 37534
        private const val TYPE_ID_METENOX_MOON_DRILL = 81826
        private val flexSizeStructures = listOf(
            TYPE_ID_ANSIBLEX_JUMP_BRIDGE,
            TYPE_ID_PHAROLUX_CYNO_BEACON,
            TYPE_ID_TENEBREX_CYNO_JAMMER,
            TYPE_ID_METENOX_MOON_DRILL,
        )

        private const val CATEGORY_ID_STATION = 3
        private const val CATEGORY_ID_STRUCTURE = 65
    }

    operator fun invoke(type: Type) = when (type.categoryId) {
        CATEGORY_ID_STATION -> true
        CATEGORY_ID_STRUCTURE if type.id !in flexSizeStructures -> true
        else -> false
    }
}
