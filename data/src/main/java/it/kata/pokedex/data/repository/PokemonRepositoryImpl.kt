package it.kata.pokedex.data.repository

import androidx.paging.PagingSource
import it.kata.pokedex.core.AppResult
import it.kata.pokedex.core.resultOf
import it.kata.pokedex.data.remote.PokeApi
import it.kata.pokedex.data.remote.PokemonNameIndexDataSource
import it.kata.pokedex.data.remote.PokemonPagingSource
import it.kata.pokedex.data.remote.mapper.toDescription
import it.kata.pokedex.data.remote.mapper.toDomain
import it.kata.pokedex.di.IoDispatcher
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonPage
import it.kata.pokedex.domain.repository.PokemonRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Builds a page of the list out of three different endpoints.
 *
 * The API gives only names and urls in the list, so every page costs one call plus two per entry:
 * forty one requests for twenty Pokemon. Two things keep that from being unusable, and both are
 * worth knowing about: the per entry calls run in parallel rather than one after the other, and the
 * OkHttp disk cache means a page already visited costs nothing on the way back.
 *
 * Browsing and searching differ only in where the twenty names come from. Everything after that,
 * which is all of the expensive part, is shared, so there is a single paging path to reason about
 * and a single one to test.
 */
class PokemonRepositoryImpl @Inject constructor(
    private val api: PokeApi,
    private val nameIndex: PokemonNameIndexDataSource,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PokemonRepository {

    override fun pokemonPagingSource(query: String): PagingSource<Int, Pokemon> =
        PokemonPagingSource { offset, limit -> getPage(query, offset, limit) }

    /**
     * Not part of [PokemonRepository]: the domain only ever asks for a paging source, so fetching
     * a single window stays an implementation detail. Visible for its own tests.
     */
    suspend fun getPage(query: String, offset: Int, limit: Int): AppResult<PokemonPage> =
        withContext(dispatcher) {
            resultOf {
                val page = if (query.isBlank()) {
                    browseNames(offset, limit)
                } else {
                    searchNames(query, offset, limit)
                }

                PokemonPage(items = hydrate(page.names), hasMore = page.hasMore)
            }
        }

    /** No query: the API paginates for us, and `next` says whether to keep going. */
    private suspend fun browseNames(offset: Int, limit: Int): NamePage {
        val page = api.getPokemonPage(limit = limit, offset = offset)
        return NamePage(
            names = page.results.orEmpty().mapNotNull { it.name?.takeIf(String::isNotBlank) },
            hasMore = page.next != null,
        )
    }

    /** With a query the matches are already in memory, so a page is just a slice of them. */
    private suspend fun searchNames(query: String, offset: Int, limit: Int): NamePage {
        val matches = nameIndex.namesMatching(query)
        return NamePage(
            names = matches.drop(offset).take(limit),
            hasMore = offset + limit < matches.size,
        )
    }

    /** The expensive half, and the reason it runs in parallel: two calls for each of twenty names. */
    private suspend fun hydrate(names: List<String>): List<Pokemon> = coroutineScope {
        names.map { name -> async { loadPokemon(name) } }
            .awaitAll()
            .filterNotNull()
    }

    private suspend fun loadPokemon(name: String): Pokemon? {
        val detail = api.getPokemonDetail(name)
        val id = detail.id ?: return null
        return detail.toDomain(description = loadDescription(id))
    }

    /**
     * A missing description is worth an empty line, not a failed page: the row still has artwork,
     * name and types. Cancellation is rethrown so a scrolled away page really does stop working.
     */
    private suspend fun loadDescription(id: Int): String =
        try {
            api.getPokemonSpecies(id).toDescription()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            ""
        }

    private class NamePage(val names: List<String>, val hasMore: Boolean)
}
