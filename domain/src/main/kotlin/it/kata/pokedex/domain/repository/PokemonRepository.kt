package it.kata.pokedex.domain.repository

import androidx.paging.PagingSource
import it.kata.pokedex.domain.model.Pokemon

/**
 * An interface so the source of the list can be swapped, for a fake in tests or for a different
 * implementation altogether, without the domain knowing where the data comes from.
 *
 * It hands back a [PagingSource] rather than a whole pager: how a page is fetched belongs to the
 * data layer, while how big a page is stays a rule of the feature and lives with the use case.
 * `androidx.paging` is a library here, not a layer, so depending on it does not cross a boundary.
 */
interface PokemonRepository {

    /** A blank [query] means the whole list; anything else narrows it down by name. */
    fun pokemonPagingSource(query: String): PagingSource<Int, Pokemon>
}
