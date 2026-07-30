package it.kata.pokedex.presentation.navigation

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/** The bar alone: testing that a tab swaps the screen would need Hilt to build the destinations. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PokedexBottomBarTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `reports the tab that was tapped`() {
        val selected = mutableListOf<PokedexDestination>()
        showBar(current = PokedexDestination.LIST, onSelect = { selected += it })

        compose.onNodeWithText("Favorites").performClick()

        assertEquals(listOf(PokedexDestination.FAVORITES), selected)
    }

    @Test
    fun `marks the tab it was told is current`() {
        showBar(current = PokedexDestination.FAVORITES)

        compose.onNodeWithText("Favorites").assertIsSelected()
    }

    private fun showBar(
        current: PokedexDestination,
        onSelect: (PokedexDestination) -> Unit = {},
    ) {
        compose.setContent {
            PokedexBottomBar(current = current, onSelect = onSelect)
        }
        compose.waitForIdle()
    }
}
