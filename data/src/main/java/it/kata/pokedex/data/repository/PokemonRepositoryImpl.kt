package it.kata.pokedex.data.repository

import androidx.paging.PagingSource
import it.kata.pokedex.core.AppResult
import it.kata.pokedex.core.resultOf
import it.kata.pokedex.data.remote.PokeApi
import it.kata.pokedex.data.remote.PokemonNameIndexDataSource
import it.kata.pokedex.data.remote.PokemonTypeIndexDataSource
import it.kata.pokedex.data.remote.PokemonPagingSource
import it.kata.pokedex.data.remote.dto.PokemonDetailDto
import it.kata.pokedex.data.remote.mapper.toDescription
import it.kata.pokedex.data.remote.mapper.toDomain
import it.kata.pokedex.di.IoDispatcher
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonPage
import it.kata.pokedex.domain.model.PokemonQuery
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.repository.PokemonRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * A page is free, a slice of the indexes already in memory. A row costs two requests, and they are
 * sequential on purpose: the address of the description is inside the detail, so running them
 * together would mean assembling that address from an id instead of reading it.
 */
class PokemonRepositoryImpl @Inject constructor(
    private val api: PokeApi,
    private val nameIndex: PokemonNameIndexDataSource,
    private val typeIndex: PokemonTypeIndexDataSource,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PokemonRepository {

    override fun pokemonPagingSource(query: PokemonQuery): PagingSource<Int, PokemonRef> =
        PokemonPagingSource { offset, limit -> getPage(query, offset, limit) }

    override suspend fun pokemon(ref: PokemonRef): AppResult<Pokemon> = withContext(dispatcher) {
        resultOf {
            val detail = api.getPokemonDetail(ref.detailUrl)

            detail.toDomain(description = descriptionOf(detail))
                ?: throw IOException("${ref.name} came back without an id or a name")
        }
    }

    /** A missing description is worth an empty line, not a blank row. */
    private suspend fun descriptionOf(detail: PokemonDetailDto): String {
        val url = detail.species?.url?.takeIf { it.isNotBlank() } ?: return ""

        return try {
            api.getPokemonSpecies(url).toDescription()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            ""
        }
    }

    /** Not part of [PokemonRepository]: an implementation detail, visible for its own tests. */
    suspend fun getPage(query: PokemonQuery, offset: Int, limit: Int): AppResult<PokemonPage> =
        withContext(dispatcher) {
            resultOf {
                val matches = matching(query)

                PokemonPage(
                    items = matches.drop(offset).take(limit),
                    hasMore = offset + limit < matches.size,
                )
            }
        }

    /** Both filters are applied to the pointers, before anything is fetched, so narrowing is free. */
    private suspend fun matching(query: PokemonQuery): List<PokemonRef> {
        val byName = nameIndex.matching(query.name)
        if (query.types.isEmpty()) return byName

        val ofSelectedTypes = typeIndex.namesOfAnyOf(query.types)
        return byName.filter { it.name in ofSelectedTypes }
    }
}
