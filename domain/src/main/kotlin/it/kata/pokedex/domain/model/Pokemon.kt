package it.kata.pokedex.domain.model

data class Pokemon(
    val id: Int,
    val name: String,
    /** Null when the API has no artwork for this entry. */
    val imageUrl: String?,
    val types: List<PokemonType>,
    val description: String,
)
