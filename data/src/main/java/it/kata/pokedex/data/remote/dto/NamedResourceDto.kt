package it.kata.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Nullable on purpose, here and in every other DTO: Gson builds objects by reflection and will
 * happily write a null into a field the Kotlin type says cannot be null. The mappers are the single
 * place that decides what a missing value means.
 */
data class NamedResourceDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String? = null,
)
