package it.kata.pokedex.domain.model

/**
 * What the user is asking the list for.
 *
 * The two halves combine with AND: a Pokemon has to match the name **and** carry one of the types.
 * The types themselves combine with OR, because a chip filter that returns nothing when you pick
 * Fire and Water reads as broken rather than as empty.
 *
 * Empty on both counts means the whole list, which is what plain browsing is.
 */
data class PokemonQuery(
    val name: String = "",
    val types: Set<PokemonType> = emptySet(),
) {
    val isEmpty: Boolean get() = name.isBlank() && types.isEmpty()
}
