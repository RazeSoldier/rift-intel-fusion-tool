package dev.nohus.rift.network.esi

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [Pagination](https://developers.eveonline.com/blog/changing-pagination-turning-a-new-page)
 */
@Serializable
data class Cursor(
    @SerialName("before")
    val before: String? = null,

    @SerialName("after")
    val after: String? = null,
)

@Serializable
abstract class CursorPaginated<T> {
    @SerialName("cursor")
    val cursor: Cursor? = null

    abstract val items: List<T>
}

suspend fun <T> fetchCursorPaginated(
    request: suspend (before: String?, after: String?) -> Result<CursorPaginated<T>>,
): Result<List<T>> {
    when (val initialResponse = request(null, null)) {
        is Failure -> return initialResponse
        is Success -> {
            return coroutineScope {
                val items = initialResponse.data.items.toMutableList()

                val beforeItemsDeferred = async {
                    val beforeItems = mutableListOf<T>()
                    var before = initialResponse.data.cursor?.before
                    while (before != null) {
                        when (val response = request(before, null)) {
                            is Failure -> return@async response
                            is Success -> {
                                beforeItems.addAll(0, response.data.items)
                                before = response.data.cursor?.before
                            }
                        }
                    }
                    Success(beforeItems)
                }

                val afterItemsDeferred = async {
                    val afterItems = mutableListOf<T>()
                    var after = initialResponse.data.cursor?.after
                    while (after != null) {
                        when (val response = request(null, after)) {
                            is Failure -> return@async response
                            is Success -> {
                                afterItems.addAll(response.data.items)
                                after = response.data.cursor?.after
                            }
                        }
                    }
                    Success(afterItems)
                }

                val beforeItems = when (val result = beforeItemsDeferred.await()) {
                    is Failure -> return@coroutineScope result
                    is Success -> result.data
                }
                val afterItems = when (val result = afterItemsDeferred.await()) {
                    is Failure -> return@coroutineScope result
                    is Success -> result.data
                }

                Success(beforeItems + items + afterItems)
            }
        }
    }
}
