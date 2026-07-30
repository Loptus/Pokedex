package it.kata.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

/** `GET /pokemon?limit=100000`, the whole list of names in one request. */
data class PokemonIndexDto(
    @SerializedName("results") val results: List<NamedResourceDto>? = null,
)
