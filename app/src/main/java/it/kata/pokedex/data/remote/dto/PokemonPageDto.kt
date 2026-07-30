package it.kata.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * `GET /pokemon?limit=20&offset=0`.
 *
 * The list only carries names and urls: sprites, types and description each need their own call.
 * [next] is what tells us whether there is another page.
 */
data class PokemonPageDto(
    @SerializedName("count") val count: Int? = null,
    @SerializedName("next") val next: String? = null,
    @SerializedName("results") val results: List<NamedResourceDto>? = null,
)
