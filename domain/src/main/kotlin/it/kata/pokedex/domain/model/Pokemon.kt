package it.kata.pokedex.domain.model

/**
 * A fully loaded Pokemon: everything one row of the list renders, and nothing else.
 *
 * It is fetched for a single row at a time, once that row is on screen, which is why the
 * description belongs here after all: two requests for one visible row is the unit of work, and
 * splitting them further would buy nothing.
 *
 * [imageUrl] is nullable because the API does not have artwork for every entry.
 */
data class Pokemon(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val types: List<PokemonType>,
    val description: String,
)
