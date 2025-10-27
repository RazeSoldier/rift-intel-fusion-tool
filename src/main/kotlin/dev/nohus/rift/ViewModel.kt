package dev.nohus.rift

import dev.nohus.rift.crash.handleFatalException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

open class ViewModel {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable -> handleFatalException(throwable) }
    protected val viewModelScope = CoroutineScope(Job() + exceptionHandler)

    fun <T> MutableSharedFlow<T>.scopedEmit(value: T) {
        viewModelScope.launch {
            emit(value)
        }
    }
}
