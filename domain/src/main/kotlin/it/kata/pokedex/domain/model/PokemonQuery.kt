package it.kata.pokedex.domain.model

/**
 * Name and types combine with AND, the types with each other with OR: a chip filter that returns
 * nothing when you pick Fire and Water reads as broken rather than as empty.
 */
data class PokemonQuery(
    val name: String = "",
    val types: Set<PokemonType> = emptySet(),
) {
    val isEmpty: Boolean get() = name.isBlank() && types.isEmpty()
}
