package it.kata.pokedex.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import it.kata.pokedex.R

/**
 * The two tabs, and everything the bottom bar needs to draw one.
 *
 * The routes are plain strings rather than the type safe ones: neither destination takes an
 * argument, so type safety would guard against a mistake that cannot be made here, at the cost of
 * the serialization plugin and its runtime. An enum keeps the two in one place, which is what
 * actually prevents a typo.
 */
enum class PokedexDestination(
    val route: String,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
) {
    LIST(route = "list", icon = R.drawable.ic_list, label = R.string.title_pokedex),
    FAVORITES(route = "favorites", icon = R.drawable.ic_favorite_filled, label = R.string.title_favorites),
}
