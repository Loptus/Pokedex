package it.kata.pokedex.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The API has no fuzzy search: `GET /pokemon/{name}` matches exactly, so typing "char" would find
 * nothing. The way around it is to fetch the full lightweight index once, names only, and filter it
 * here.
 *
 * The whole index is a single request of a few tens of kilobytes, which is far cheaper than it
 * sounds, and it is kept in memory rather than in Room: it is derived data that has to be refetched
 * whenever the API grows, so persisting it would only add a cache to invalidate.
 */
private const val INDEX_LIMIT = 100_000

@Singleton
class PokemonNameIndexDataSource @Inject constructor(
    private val api: PokeApi,
) {

    private val mutex = Mutex()
    private var names: List<String>? = null

    /** Names containing [query], in the order the API lists them, so results stay by Pokedex number. */
    suspend fun namesMatching(query: String): List<String> =
        index().filter { it.contains(query, ignoreCase = true) }

    /**
     * The download happens while holding the lock on purpose: callers that arrive during the first
     * fetch wait for it instead of each firing a request of their own. A failure leaves the index
     * unset, so the next search tries again rather than caching the emptiness.
     */
    private suspend fun index(): List<String> = mutex.withLock {
        names ?: api.getPokemonPage(limit = INDEX_LIMIT, offset = 0)
            .results.orEmpty()
            .mapNotNull { it.name?.takeIf(String::isNotBlank) }
            .also { names = it }
    }
}
