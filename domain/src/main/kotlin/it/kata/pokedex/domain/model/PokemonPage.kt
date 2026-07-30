package it.kata.pokedex.domain.model

/**
 * One page of the list: pointers only, since the rows fill themselves in later.
 *
 * [hasMore] is worked out from how many entries the source holds, not from the size of [items]:
 * entries that cannot be read are dropped, so a short page does not mean the list is over.
 */
data class PokemonPage(
    val items: List<PokemonRef>,
    val hasMore: Boolean,
)
