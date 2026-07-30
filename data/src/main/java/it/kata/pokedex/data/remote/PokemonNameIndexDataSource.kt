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
 * Every Pokemon the API knows about, fetched once and then answered from memory.
 *
 * Two reasons for taking the whole thing instead of paging it. The first is that the API has no
 * fuzzy search, so `GET /pokemon/{name}` would never find "char": filtering locally is the only way
 * to search at all. The second is arithmetic, measured against the real API and gzipped: the entire
 * index is about 11.7 KB, while the two requests that fill a single row are 12 KB. Paging the index
 * would optimise the one part that costs nothing.
 *
 * Kept in memory rather than in Room: it is derived data that has to be refetched whenever the API
 * grows, so persisting it would only add a cache to invalidate.
 */
@Singleton
class PokemonNameIndexDataSource @Inject constructor(
    private val api: PokeApi,
) {

    private val mutex = Mutex()
    private var refs: List<PokemonRef>? = null

    /**
     * Entries whose name contains [query], in the order the API lists them, so results stay by
     * Pokedex number. A blank query matches everything, which is what plain browsing asks for.
     */
    suspend fun matching(query: String): List<PokemonRef> {
        val all = index()
        return if (query.isBlank()) {
            all
        } else {
            all.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    /**
     * The download happens while holding the lock on purpose: callers that arrive during the first
     * fetch wait for it instead of each firing a request of their own. A failure leaves the index
     * unset, so the next attempt tries again rather than caching the emptiness.
     */
    private suspend fun index(): List<PokemonRef> = mutex.withLock {
        refs ?: api.getPokemonIndex(limit = INDEX_LIMIT)
            .results.orEmpty()
            .mapNotNull { it.toRef() }
            .also { refs = it }
    }
}
