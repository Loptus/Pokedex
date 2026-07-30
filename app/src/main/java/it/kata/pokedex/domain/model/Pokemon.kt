package it.kata.pokedex.domain.model

/**
 * A Pokemon as the app needs it, nothing more: only the fields the list row actually renders.
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
