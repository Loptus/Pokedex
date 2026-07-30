package it.kata.pokedex.domain.repository

import androidx.paging.PagingSource
import it.kata.pokedex.core.AppResult
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonQuery
import it.kata.pokedex.domain.model.PokemonRef

/**
 * Hands back a [PagingSource] rather than a whole pager: how a page is fetched belongs to the data
 * layer, while how big a page is stays a rule of the feature and lives with the use case.
 */
interface PokemonRepository {

    fun pokemonPagingSource(query: PokemonQuery): PagingSource<Int, PokemonRef>

    suspend fun pokemon(ref: PokemonRef): AppResult<Pokemon>
}
