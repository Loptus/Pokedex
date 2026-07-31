package it.kata.pokedex.data.remote

import it.kata.pokedex.data.remote.mapper.toRef
import it.kata.pokedex.domain.model.PokemonRef
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** High enough to take the whole list in one request: there were 1351 entries at the time of writing. */
private const val INDEX_LIMIT = 100_000

/**
 * The whole index in one request, then answered from memory. The API has no fuzzy search, so
 * filtering locally is the only way to search at all, and gzipped the entire index is 11.7 KB
 * against the 12 KB that one row costs: paging it would optimise the part that is already free.
 */
@Singleton
class PokemonNameIndexDataSource @Inject constructor(
    private val api: PokeApi,
) {

    private val mutex = Mutex()
    private var refs: List<PokemonRef>? = null

    suspend fun matching(query: String): List<PokemonRef> {
        val all = index()
        // Trimmed here rather than at the caller so no route into the index can skip it: keyboards
        // add a trailing space often enough that "pika " would otherwise find nothing.
        val term = query.trim()

        return if (term.isEmpty()) {
            all
        } else {
            all.filter { it.name.contains(term, ignoreCase = true) }
        }
    }

    /**
     * The download happens while holding the lock on purpose: callers arriving during the first
     * fetch wait for it instead of each firing a request of their own. A failure leaves the index
     * unset, so the next attempt retries instead of caching the emptiness.
     */
    private suspend fun index(): List<PokemonRef> = mutex.withLock {
        refs ?: api.getPokemonIndex(limit = INDEX_LIMIT)
            .results.orEmpty()
            .mapNotNull { it.toRef() }
            .also { refs = it }
    }
}
