package it.kata.pokedex.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import it.kata.pokedex.R

/**
 * Plain string routes rather than the type safe ones: neither destination takes an argument, so type
 * safety would cost the serialization plugin to guard against a mistake that cannot be made here.
 */
enum class PokedexDestination(
    val route: String,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
) {
    LIST(route = "list", icon = R.drawable.ic_list, label = R.string.title_pokedex),
    FAVORITES(route = "favorites", icon = R.drawable.ic_favorite_filled, label = R.string.title_favorites),
}
