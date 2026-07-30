package it.kata.pokedex.domain.repository

import androidx.paging.PagingSource
import it.kata.pokedex.core.AppResult
import it.kata.pokedex.domain.model.Pokemon
import it.kata.pokedex.domain.model.PokemonRef

/**
 * An interface so the source of the list can be swapped, for a fake in tests or for a different
 * implementation altogether, without the domain knowing where the data comes from.
 *
 * It hands back a [PagingSource] rather than a whole pager: how a page is fetched belongs to the
 * data layer, while how big a page is stays a rule of the feature and lives with the use case.
 * `androidx.paging` is a library here, not a layer, so depending on it does not cross a boundary.
 */
interface PokemonRepository {

    /**
     * Pages through the names alone. A blank [query] means the whole list; anything else narrows it
     * down by name.
     */
    fun pokemonPagingSource(query: String): PagingSource<Int, PokemonRef>

    /**
     * Everything a row shows, for one Pokemon.
     *
     * Called per row, once that row is on screen. Cancelling the caller cancels the requests, which
     * is how scrolling quickly past a row stops paying for it.
     */
    suspend fun pokemon(ref: PokemonRef): AppResult<Pokemon>
}
