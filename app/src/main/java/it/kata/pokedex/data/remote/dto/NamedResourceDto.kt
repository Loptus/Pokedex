package it.kata.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * The `{ name, url }` pair the API uses everywhere: list entries, type references, languages.
 *
 * Every field is nullable on purpose. Gson builds objects by reflection and happily writes a null
 * into a field the Kotlin type says cannot be null, so a default here would be a false promise.
 * The mappers are the single place that decides what a missing value means.
 */
data class NamedResourceDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String? = null,
)
