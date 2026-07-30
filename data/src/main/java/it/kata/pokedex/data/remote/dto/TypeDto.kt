package it.kata.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

/** `GET /type/{name}`, cut down to the one field the filter needs: who has this type. */
data class TypeDto(
    @SerializedName("pokemon") val pokemon: List<TypeMemberDto>? = null,
)

data class TypeMemberDto(
    @SerializedName("pokemon") val pokemon: NamedResourceDto? = null,
)
