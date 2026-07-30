package it.kata.pokedex.core

import kotlin.coroutines.cancellation.CancellationException

sealed interface AppResult<out T> {
    data class Success<out T>(val value: T) : AppResult<T>
    data class Failure(val cause: Throwable) : AppResult<Nothing>
}

/**
 * [CancellationException] is rethrown on purpose: catching it would report a failure for work that
 * was cancelled, and would break structured concurrency.
 */
suspend fun <T> resultOf(block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        AppResult.Failure(error)
    }
