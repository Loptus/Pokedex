package it.kata.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * `GET /pokemon?limit=100000`, the whole list of names in one request.
 *
 * `count`, `next` and `previous` are ignored: asking for everything at once makes them redundant,
 * and the number of entries is simply the size of [results].
 */
data class PokemonIndexDto(
    @SerializedName("results") val results: List<NamedResourceDto>? = null,
)
