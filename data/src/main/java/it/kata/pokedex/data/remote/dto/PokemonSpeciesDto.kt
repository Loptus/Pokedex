package it.kata.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * `GET /pokemon-species/{id}`, the only place the description lives.
 *
 * The entries repeat once per game and per language, so they have to be filtered before use.
 */
data class PokemonSpeciesDto(
    @SerializedName("flavor_text_entries") val flavorTextEntries: List<FlavorTextEntryDto>? = null,
)

data class FlavorTextEntryDto(
    @SerializedName("flavor_text") val flavorText: String? = null,
    @SerializedName("language") val language: NamedResourceDto? = null,
)
