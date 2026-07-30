package it.kata.pokedex.domain.model

/**
 * [detailUrl] is carried rather than rebuilt from [id]: from 10001 onwards the entries are alternate
 * forms whose species lives under a different, lower id, so a url assembled from an id is wrong for
 * all of them.
 */
data class PokemonRef(
    val id: Int,
    val name: String,
    val detailUrl: String,
)
