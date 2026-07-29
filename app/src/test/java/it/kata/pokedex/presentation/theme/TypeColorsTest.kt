package it.kata.pokedex.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import it.kata.pokedex.domain.model.PokemonType
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypeColorsTest {

    @Test
    fun `every type has a colour of its own`() {
        val colours = PokemonType.entries.map { it.color }
        assertEquals(PokemonType.entries.size, colours.toSet().size)
    }

    /**
     * The reason the pivot is 0.179 and not the intuitive 0.5. WCAG asks for 4.5:1 on small text,
     * and a chip label is small text: a wrong pivot silently ships white on electric yellow.
     */
    @Test
    fun `every chip label stays readable on its own background`() {
        PokemonType.entries.forEach { type ->
            val background = type.color
            val ratio = contrastRatio(background, contentColorOn(background))
            assertTrue(
                ratio >= 4.5f,
                "${type.apiName} only reaches ${"%.2f".format(ratio)}:1",
            )
        }
    }

    @Test
    fun `picks black on light backgrounds and white on dark ones`() {
        assertEquals(Color.Black, contentColorOn(Color.White))
        assertEquals(Color.White, contentColorOn(Color.Black))
    }

    private fun contrastRatio(a: Color, b: Color): Float {
        val lighter = max(a.luminance(), b.luminance())
        val darker = min(a.luminance(), b.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
