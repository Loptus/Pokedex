package it.kata.pokedex.presentation.list

import it.kata.pokedex.presentation.common.previewRefs

/** The shared fixtures as this screen receives them, one of them saved, to preview both hearts. */
internal val previewItems = previewRefs.mapIndexed { index, ref ->
    PokemonListItem(ref = ref, isFavorite = index == 0)
}
