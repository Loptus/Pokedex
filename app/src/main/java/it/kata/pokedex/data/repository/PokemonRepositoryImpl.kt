package it.kata.pokedex.data.repository

import androidx.paging.PagingSource
import it.kata.pokedex.core.AppResult
import it.kata.pokedex.core.resultOf
import it.kata.pokedex.data.remote.PokeApi
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
 */
class PokemonRepositoryImpl @Inject constructor(
    private val api: PokeApi,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PokemonRepository {

    override fun pokemonPagingSource(): PagingSource<Int, Pokemon> =
        PokemonPagingSource(loadPage = ::getPage)

    /**
     * Not part of [PokemonRepository]: the domain only ever asks for a paging source, so fetching
     * a single window stays an implementation detail. Visible for its own tests.
     */
    suspend fun getPage(offset: Int, limit: Int): AppResult<PokemonPage> =
        withContext(dispatcher) {
            resultOf {
                val page = api.getPokemonPage(limit = limit, offset = offset)

                val items = coroutineScope {
                    page.results.orEmpty()
                        .mapNotNull { it.name?.takeIf(String::isNotBlank) }
                        .map { name -> async { loadPokemon(name) } }
                        .awaitAll()
                        .filterNotNull()
                }

                PokemonPage(items = items, hasMore = page.next != null)
            }
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
}
