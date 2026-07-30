package it.kata.pokedex.domain.model

/**
 * A pointer to a Pokemon: everything the index knows about it, which is enough to key a list row and
 * to go and ask for the rest.
 *
 * [detailUrl] is carried rather than rebuilt from [id], and that is the whole point of this type.
 * The API is a web of links, and its ids do not line up: from 10001 onwards the entries are
 * alternate forms whose species lives under an entirely different, lower id, so any url assembled
 * out of an id is wrong for them. Following the link the API gave us cannot be wrong.
 *
 * The list is built out of these rather than out of [Pokemon] because the index is already in
 * memory: a page of refs costs nothing, while a page of [Pokemon] would cost two requests per row
 * the user may scroll straight past.
 */
data class PokemonRef(
    val id: Int,
    val name: String,
    val detailUrl: String,
)
