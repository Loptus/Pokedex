package it.kata.pokedex.presentation.common

import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.domain.model.PokemonType

/**
 * The API returns lowercase identifiers ("bulbasaur", "grass"). Capitalising them is a
 * presentation concern, so it lives here and not in the domain model.
 *
 * Type labels are deliberately not translated: they are values coming from the API, not interface
 * text. Translating them would mean maintaining eighteen entries per language by hand and drifting
 * from the vocabulary the API itself uses for searching.
 */
val PokemonType.label: String get() = apiName.capitalizeFirst()

/**
 * Taken from the ref rather than from the loaded Pokemon: the name is known from the index, so it
 * can be on screen before the row's own request comes back.
 */
val PokemonRef.displayName: String get() = name.capitalizeFirst()

private fun String.capitalizeFirst(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
