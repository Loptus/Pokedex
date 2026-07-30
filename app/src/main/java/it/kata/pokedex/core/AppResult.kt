package it.kata.pokedex.core

import kotlin.coroutines.cancellation.CancellationException

/**
 * Outcome of an operation that can fail, so failures travel as values instead of as exceptions
 * thrown at whoever happens to be calling.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val value: T) : AppResult<T>
    data class Failure(val cause: Throwable) : AppResult<Nothing>
}

/**
 * Runs [block] and turns a failure into [AppResult.Failure].
 *
 * [CancellationException] is rethrown on purpose: catching it would tell the caller the work
 * failed when in fact it was cancelled, and would break structured concurrency.
 */
suspend fun <T> resultOf(block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        AppResult.Failure(error)
    }
