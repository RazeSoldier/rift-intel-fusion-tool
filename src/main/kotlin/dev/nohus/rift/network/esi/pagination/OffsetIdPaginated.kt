package dev.nohus.rift.network.esi.pagination

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success

interface OffsetId {
    val offsetId: Long
}

suspend fun <T : OffsetId> fetchOffsetIdPaginated(
    request: suspend (fromId: Long?) -> Result<List<T>>,
): Result<List<T>> {
    val items = mutableListOf<T>()
    var fromId: Long? = null
    while (true) {
        when (val result = request(fromId)) {
            is Success -> {
                val newItems = result.data.filter { it.offsetId != fromId }
                items += newItems
                if (newItems.isEmpty()) break
                fromId = newItems.minOf { it.offsetId }
            }
            is Failure -> return result
        }
    }
    return Success(items)
}
