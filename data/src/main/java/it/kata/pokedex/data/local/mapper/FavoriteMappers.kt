package it.kata.pokedex.data.local.mapper

import it.kata.pokedex.data.local.FavoritePokemonEntity
import it.kata.pokedex.domain.model.PokemonRef

fun PokemonRef.toEntity(): FavoritePokemonEntity = FavoritePokemonEntity(
    id = id,
    name = name,
    detailUrl = detailUrl,
)

fun FavoritePokemonEntity.toDomain(): PokemonRef = PokemonRef(
    id = id,
    name = name,
    detailUrl = detailUrl,
)
