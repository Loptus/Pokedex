package it.kata.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * `GET /pokemon/{name}`, cut down to what the list row needs.
 *
 * The real payload is large: everything not declared here is simply ignored, which keeps parsing
 * cheap and the DTO readable.
 */
data class PokemonDetailDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("types") val types: List<TypeSlotDto>? = null,
    @SerializedName("sprites") val sprites: SpritesDto? = null,
    // Where the description lives. Following this url is the only reliable way to get there:
    // the species of an alternate form sits under a different id than the form itself.
    @SerializedName("species") val species: NamedResourceDto? = null,
)

data class TypeSlotDto(
    @SerializedName("type") val type: NamedResourceDto? = null,
)

data class SpritesDto(
    @SerializedName("front_default") val frontDefault: String? = null,
    @SerializedName("other") val other: OtherSpritesDto? = null,
)

data class OtherSpritesDto(
    @SerializedName("official-artwork") val officialArtwork: ArtworkDto? = null,
)

data class ArtworkDto(
    @SerializedName("front_default") val frontDefault: String? = null,
)
