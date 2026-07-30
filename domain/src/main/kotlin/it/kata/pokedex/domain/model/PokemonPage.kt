package it.kata.pokedex.domain.model

/**
 * One page of the list.
 *
 * [hasMore] is worked out from how many entries the source holds, not from the size of [items]:
 * entries that cannot be parsed are dropped, so a short page does not mean the list is over.
 */
data class PokemonPage(
    val items: List<Pokemon>,
    val hasMore: Boolean,
)
