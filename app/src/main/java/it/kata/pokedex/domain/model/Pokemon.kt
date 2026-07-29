package it.kata.pokedex.domain.model

/**
 * A Pokemon as the app needs it, nothing more: only the fields the list row and the favourites
 * screen actually render.
 *
 * The description is not part of this model on purpose. It lives on a separate PokeAPI endpoint
 * and is loaded lazily, per row, so tying it to the model would force every caller to pay for it.
 *
 * [imageUrl] is nullable because the API does not have artwork for every entry.
 */
data class Pokemon(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val types: List<PokemonType>,
)
