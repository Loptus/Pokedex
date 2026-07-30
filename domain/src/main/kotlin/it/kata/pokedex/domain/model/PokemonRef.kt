package it.kata.pokedex.domain.model

/**
 * A pointer to a Pokemon: everything the index knows about it, which is enough to key a list row and
 * to go and ask for the rest.
 *
 * The list is built out of these rather than out of [Pokemon] because the index is already in
 * memory: a page of refs costs nothing, while a page of [Pokemon] would cost forty requests for
 * twenty rows the user may scroll straight past.
 */
data class PokemonRef(
    val id: Int,
    val name: String,
)
