package it.kata.pokedex.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** High enough to take the whole list in one request: there were 1351 entries at the time of writing. */
private const val INDEX_LIMIT = 100_000

/**
 * The list of every Pokemon name, fetched once and then answered from memory.
 *
 * Two reasons for taking the whole thing instead of paging it. The first is that the API has no
 * fuzzy search, so `GET /pokemon/{name}` would never find "char": filtering locally is the only way
 * to search at all. The second is arithmetic, measured against the real API and gzipped: the entire
 * index is about 11.7 KB, a page of twenty names is 308 bytes, and the twenty details and species
 * that actually fill that page are 240 KB. Paging the names optimises the one part that costs
 * nothing, and it makes the first search wait for a download that could already have happened.
 *
 * Kept in memory rather than in Room: it is derived data that has to be refetched whenever the API
 * grows, so persisting it would only add a cache to invalidate.
 */
@Singleton
class PokemonNameIndexDataSource @Inject constructor(
    private val api: PokeApi,
) {

    private val mutex = Mutex()
    private var names: List<String>? = null

    /**
     * Names containing [query], in the order the API lists them, so results stay by Pokedex number.
     * A blank query matches everything, which is what plain browsing asks for.
     */
    suspend fun namesMatching(query: String): List<String> {
        val all = index()
        return if (query.isBlank()) all else all.filter { it.contains(query, ignoreCase = true) }
    }

    /**
     * The download happens while holding the lock on purpose: callers that arrive during the first
     * fetch wait for it instead of each firing a request of their own. A failure leaves the index
     * unset, so the next attempt tries again rather than caching the emptiness.
     */
    private suspend fun index(): List<String> = mutex.withLock {
        names ?: api.getPokemonIndex(limit = INDEX_LIMIT)
            .results.orEmpty()
            .mapNotNull { it.name?.takeIf(String::isNotBlank) }
            .also { names = it }
    }
}
