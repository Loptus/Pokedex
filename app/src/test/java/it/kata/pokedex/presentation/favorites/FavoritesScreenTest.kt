package it.kata.pokedex.presentation.favorites

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import it.kata.pokedex.domain.model.PokemonRef
import it.kata.pokedex.presentation.common.PokemonRowState
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
// Robolectric has no image for SDK 37 yet, and the app targets it.
@Config(sdk = [36])
class FavoritesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val bulbasaur = PokemonRef(id = 1, name = "bulbasaur", detailUrl = "url/1")
    private val charmander = PokemonRef(id = 4, name = "charmander", detailUrl = "url/4")

    /** Someone with favorites must not be told, even for a frame, that they have none. */
    @Test
    fun `says nothing before the database has answered`() {
        showFavorites(favorites = null)

        compose.onNodeWithText("No favorites yet").assertDoesNotExist()
    }

    @Test
    fun `says how to save one when nothing is saved`() {
        showFavorites(favorites = emptyList())

        compose.onNodeWithText("No favorites yet").assertIsDisplayed()
    }

    @Test
    fun `shows the saved entries`() {
        showFavorites(favorites = listOf(bulbasaur, charmander))

        compose.onNodeWithText("Bulbasaur").assertIsDisplayed()
        compose.onNodeWithText("Charmander").assertIsDisplayed()
    }

    @Test
    fun `the heart removes the row it belongs to`() {
        val removed = mutableListOf<Int>()
        showFavorites(favorites = listOf(bulbasaur, charmander), onRemove = { removed += it })

        compose.onNodeWithContentDescription("Remove Charmander from favorites").performClick()

        assertEquals(listOf(4), removed)
    }

    private fun showFavorites(
        favorites: List<PokemonRef>?,
        onRemove: (Int) -> Unit = {},
    ) {
        compose.setContent {
            FavoritesScreen(
                favorites = favorites,
                onRemove = onRemove,
                rowFor = { PokemonRowState.Loading },
            )
        }
        compose.waitForIdle()
    }
}
