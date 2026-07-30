package it.kata.pokedex.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * `GET /type/{name}`, cut down to the one field the filter needs: who has this type.
 *
 * The payload also carries damage relations, moves and generations. Leaving them undeclared keeps
 * a type down to a few kilobytes once parsed.
 */
data class TypeDto(
    @SerializedName("pokemon") val pokemon: List<TypeMemberDto>? = null,
)

data class TypeMemberDto(
    @SerializedName("pokemon") val pokemon: NamedResourceDto? = null,
)
